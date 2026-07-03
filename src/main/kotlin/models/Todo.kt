package com.prosenjith.todoflow.models

import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

@Serializable
data class CreateTodoRequest(
    val title: String
)

@Serializable
data class UpdateTodoRequest(
    val title: String? = null,
    val completed: Boolean? = null
)