/**
  * Copyright [2017] [B2W Digital]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
  */
package org.marvin.executor.statemachine

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{ActorRef, ActorSystem, FSM, OneForOneStrategy, Props}
import org.marvin.executor.actions.OnlineAction
import org.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload, OnlineReloadNoSave}
import org.marvin.executor.api.GenericHttpAPI
import org.marvin.model.MarvinEExecutorException

import scala.concurrent.duration._
import scala.util.Failure

//receive events
final case class Reload(protocol: String = "")
final case class ReloadNoSave(protocol: String = "")
final case class Reloaded(protocol: String)

//states
sealed trait State
case object Unavailable extends State
case object Reloading extends State
case object Ready extends State

sealed trait Data
case object NoModel extends Data
final case class ToReload(protocol: String) extends Data
final case class Model(protocol: String) extends Data

class PredictorFSM(var predictorActor: ActorRef) extends FSM[State, Data]{

  def this() = this(null)

  var reloadStateTimeout: FiniteDuration = _

  override def preStart(): Unit = {
    if(predictorActor == null)
      predictorActor = context.system.actorOf(Props(new OnlineAction("predictor", GenericHttpAPI.metadata)), name = "predictorActor")
    reloadStateTimeout = GenericHttpAPI.metadata.reloadStateTimeout.getOrElse(180000) milliseconds
  }
  startWith(Unavailable, NoModel)

  when(Unavailable) {
    case Event(ReloadNoSave(protocol), _) => {
      predictorActor ! OnlineReloadNoSave(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(Reload(protocol), _) => {
      predictorActor ! OnlineReload(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(e,s) => {
      log.warning("Engine is unavailable, not possible to perform event {} in state {}/{}", e, stateName, s)
      sender ! akka.actor.Status.Failure(new MarvinEExecutorException(
        "It's not possible to process the request now, the model is unavailable. Perform a reload and try again."))
      stay
    }
  }

  when(Reloading, stateTimeout = reloadStateTimeout) {
    case Event(Reloaded(protocol), _) => {
      goto(Ready) using Model(protocol)
    }
    case Event(e,s) => {
      log.warning("Engine is reloading, not possible to perform event {} in state {}/{}", e, stateName, s)
      sender ! akka.actor.Status.Failure(new MarvinEExecutorException(
        "It's not possible to process the request now, the model is being reloaded."))
      stay
    }
    case Event(StateTimeout, _) => {
      log.warning("Reloading state timed out.")
      goto(Unavailable)
    }
  }

  when(Ready) {
    case Event(OnlineExecute(message, params), _) => {
      predictorActor forward OnlineExecute(message, params)
      stay
    }
    case Event(Reload(protocol), _) => {
      predictorActor ! OnlineReload(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(ReloadNoSave(protocol), _) => {
      predictorActor ! OnlineReloadNoSave(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(OnlineHealthCheck(), _) => {
      predictorActor forward OnlineHealthCheck
      stay
    }
  }

  whenUnhandled {
    case Event(e, s ) =>
      log.warning("Received an unknown event {}. The current state/data is {}/{}.", e, stateName, s)
      stay
  }

  onTransition{
    case Ready -> Reloading =>
      log.info("Received a message to reload the model.")
    case Reloading -> Ready =>
      log.info("Reloaded the model with success.")
  }

  initialize()

}
