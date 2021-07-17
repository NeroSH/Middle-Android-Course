package ru.skillbranch.skillarticles.viewmodels

import android.os.Bundle

interface IViewModelState {
    fun save(outState: Bundle)
    fun restore(savedState: Bundle): IViewModelState
}