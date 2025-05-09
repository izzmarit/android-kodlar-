package com.example.kuluckakontrolu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kuluckakontrolu.repository.IncubatorRepository

class IncubatorViewModelFactory(private val repository: IncubatorRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncubatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IncubatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}