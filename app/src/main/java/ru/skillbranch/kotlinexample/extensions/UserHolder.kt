package ru.skillbranch.kotlinexample.extensions

import ru.skillbranch.kotlinexample.User

object UserHolder {
    private val map = mutableMapOf<String, User>()
    fun  registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map.containsKey(email)) {
                    throw IllegalArgumentException("A user with this email already exists")
                } else { map[user.login] = user }
            }
    }


    fun loginUser(login: String, password: String): String? {
        return map[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String) {

    }
}