package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
//    private val firstName: String
//    private val lastName: String?
    internal val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    internal var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    internal var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private val salt: String by lazy {
        ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    }
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // by email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Seconrary email constuctor")
        passwordHash = encrypt(password)
    }


    // by phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Seconrary phone constuctor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }


    init {
        println("Init block called ")

        check(!firstName.isBlank()) { "First Name must not be empty" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email must not be empty" }
        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()


    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("Old password that are nominated to change didn't match current")
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val symbols = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (symbols.indices).random().also {
                    append(symbols[it])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("Sending access code $code to $phone ")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateNewAccessCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }

    companion object Factory {
        @Throws(IllegalArgumentException::class)
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return when{
                !phone.isNullOrBlank() -> User(firstName,lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
//                !email.isValidEmail() -> throw IllegalArgumentException("A user with this email already exists")
                else -> throw IllegalArgumentException("Email or phone must not be empty")
            }
        }

    }
}

private fun String.fullNameToPair(): Pair<String, String?> {
    return this.split(" ")
        .filter { it.isNotBlank() }
        .run {
            when(size) {
                1 -> first() to null
                2 -> first() to last()
                else -> throw java.lang.IllegalArgumentException("Fullname can contain only " +
                        "firstname and lastname. Given ${this@fullNameToPair}. ")
            }
        }
}
