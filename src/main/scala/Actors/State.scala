package Actors

import akka.util.ByteString

import scala.collection.immutable.HashSet

sealed trait State {}

case class GenesisState(accountsInfo: HashSet[Account]) extends State

case class FunctioningState(accountsInfo: HashSet[Account]) extends State

case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)