package com.example.cahyo_research_two_bg_service

import android.content.Context
import com.example.cahyo_research_two_bg_service.Constant.Companion.NOTIFICATION_STORAGE_KEY
import com.orhanobut.hawk.Hawk

class Storage {
    companion object {

        @JvmStatic
        fun initValue(context: Context) {
            Hawk.init(context).build()

            // init some props
            if (!Hawk.contains(NOTIFICATION_STORAGE_KEY)) {
                saveValue(NOTIFICATION_STORAGE_KEY, listOf<String>())
            }
        }

        @JvmStatic
        fun getValue(key: String): Any? {
            if (!Hawk.contains(key)) {
                return null
            }

            return Hawk.get(key)
        }

        @JvmStatic
        fun deleteValue(key: String? = null) {
            if (key == null) {
                Hawk.deleteAll()
            } else {
                Hawk.delete(key)
            }
        }

        @JvmStatic
        fun saveValue(key: String, param: Any) {
            Hawk.put(key, param)
        }
    }
}