package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email.validateLogin(), password = password)
            .also { user ->
                if (map.containsKey(user.login)) {
                    throw IllegalArgumentException("A user with this email already exists")
                } else {
                    map[user.login] = user
                }
            }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        return User.makeUser(fullName, phone = rawPhone.validateLogin())
            .also { user ->
                when {
                    user.phone?.length ?: 0 != 12 -> throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
                    map.containsKey(user.login) -> throw IllegalArgumentException("A user with this phone already exists")
                    else -> map[user.login] = user
                }
            }
    }

    fun loginUser(login: String, password: String): String? {
        val result = map[login.trim().validateLogin()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
        return result
    }

    fun requestAccessCode(login: String) {
        map[login.run {
            this.replace("[^+\\d]".toRegex(), "")
        }]?.also { user ->
            user.generateNewAccessCode()
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val users = mutableListOf<User>()
        for (u in list) {
            val filteredUser = u.trim().split(";")
            users.add(
                User.makeUser(
                    fullName = filteredUser[0],
                    email = filteredUser[1],
                    password = filteredUser[2],
                    phone = filteredUser[3]
                ).also {
                    map[it.login] = it
                }
            )
        }
        return users
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

}

private fun String.validateLogin(): String {
    return when {
        this.matches("[A-Za-z0-9. %+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,4}".toRegex()) -> this.toLowerCase()
        this.matches("^\\+((\\()*([0-9]+)(\\))*\\s*-*)*$".toRegex()) -> this.replace(
            "[^+\\d]".toRegex(),
            ""
        )
        else -> this
    }
}

