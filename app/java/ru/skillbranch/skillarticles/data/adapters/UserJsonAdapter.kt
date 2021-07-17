package ru.skillbranch.skillarticles.data.adapters

import org.json.JSONObject
import ru.skillbranch.skillarticles.data.local.User
import ru.skillbranch.skillarticles.extensions.asMap

class UserJsonAdapter() : JsonAdapter<User> {
    override fun fromJson(json: String): User? {
        json.ifEmpty { return null }
        val jsonObject = JSONObject(json)
        return User(
            id = jsonObject.getString("id"),
            name = jsonObject.getString("name"),
            avatar = jsonObject.opt("avatar") as? String,
            rating = jsonObject.getInt("rating"),
            respect = jsonObject.getInt("respect"),
            about = jsonObject.opt("about") as? String,
        )
    }

    override fun toJson(obj: User?): String {
        obj ?: return ""
        val jsonObject = JSONObject()
        obj.asMap().forEach {
            jsonObject.put(it.key, it.value)
        }
        return jsonObject.toString()
    }
}