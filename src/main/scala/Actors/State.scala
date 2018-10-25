package Actors

import akka.util.ByteString

import scala.collection.immutable.HashMap

sealed trait State {}

case class GenesisState(accountsInfo: HashMap[ByteString, Account]) extends State

case class FunctioningState(accountsInfo: HashMap[ByteString, Account]) extends State

case class Account(publicKey: ByteString, data: List[ByteString], nonce: Long)