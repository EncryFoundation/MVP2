package mvp2.utils

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security._
import java.security.interfaces.{ECPublicKey => JSPublicKey}
import java.security.spec.{ECGenParameterSpec, ECPublicKeySpec}
import java.util
import akka.util.ByteString
import org.bouncycastle.asn1.{ASN1EncodableVector, ASN1Integer, DEROutputStream, DERSequence}
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECNamedCurveParameterSpec, ECNamedCurveSpec}
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}
import sun.security.util.{DerInputStream, DerValue}

object ECDSA {

  Security.addProvider(new BouncyCastleProvider)

  def createKeyPair: KeyPair = {
    val keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")
    val params: ECGenParameterSpec = new ECGenParameterSpec("secp256k1")
    keyPairGenerator.initialize(params)
    keyPairGenerator.generateKeyPair
  }

  def sign(privateKey: PrivateKey, messageToSign: ByteString): ByteString = {
    val ecdsaSign: Signature = Signature.getInstance("SHA256withECDSA")
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(messageToSign.toArray)
    ECDSA.compressSignature(ByteString(ecdsaSign.sign))
  }

  def compressSignature(signatureToCompress: ByteString): ByteString = {
    val derInputStream: DerInputStream = new DerInputStream(signatureToCompress.toArray)
    val values: Array[DerValue] = derInputStream.getSequence(2)
    val random: Array[Byte] = values(0).getPositiveBigInteger.toByteArray
    val signature: Array[Byte] = values(1).getPositiveBigInteger.toByteArray
    val tokenSignature: Array[Byte] = new Array[Byte](64)
    System.arraycopy(random,
      if (random.length > 32) 1 else 0,
      tokenSignature,
      if (random.length < 32) 1 else 0,
      if (random.length > 32) 32 else random.length
    )
    System.arraycopy(signature,
      if (signature.length > 32) 1 else 0,
      tokenSignature,
      if (signature.length < 32) 33 else 32,
      if (signature.length > 32) 32 else signature.length
    )
    ByteString(tokenSignature)
  }

  def uncompressSignature(compressedSignature: ByteString): ByteString = {
    val r: Array[Byte] =
      util.Arrays.copyOfRange(compressedSignature.toArray, 0, compressedSignature.length / 2)
    val s: Array[Byte] =
      util.Arrays.copyOfRange(compressedSignature.toArray, compressedSignature.length / 2, compressedSignature.length)

    val byteArrayOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream
    val derOutputStream: DEROutputStream = new DEROutputStream(byteArrayOutputStream)
    val v: ASN1EncodableVector = new ASN1EncodableVector
    v.add(new ASN1Integer(new BigInteger(1, r)))
    v.add(new ASN1Integer(new BigInteger(1, s)))
    derOutputStream.writeObject(new DERSequence(v))
    ByteString(byteArrayOutputStream.toByteArray)
  }

  def compressPublicKey(publicKey: PublicKey): ByteString =
    ByteString(publicKey.asInstanceOf[ECPublicKey].getQ.getEncoded(true))

  def uncompressPublicKey(compressedPublicKey: ByteString): PublicKey = {
    val spec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val kf: KeyFactory = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider)
    val params: ECNamedCurveSpec =
      new ECNamedCurveSpec("secp256k1", spec.getCurve, spec.getG, spec.getN)
    val pubKeySpec: ECPublicKeySpec =
      new ECPublicKeySpec(ECPointUtil.decodePoint(params.getCurve, compressedPublicKey.toArray), params)
    kf.generatePublic(pubKeySpec).asInstanceOf[JSPublicKey]
  }

  def verify(signature: ByteString, message: ByteString, publicKey: ByteString): Boolean = {
    val ecdsaVerify: Signature = Signature.getInstance("SHA256withECDSA")
    ecdsaVerify.initVerify(uncompressPublicKey(publicKey))
    ecdsaVerify.update(message.toArray)
    ecdsaVerify.verify(ECDSA.uncompressSignature(signature).toArray)
  }
}