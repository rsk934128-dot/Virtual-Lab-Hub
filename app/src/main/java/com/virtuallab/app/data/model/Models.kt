package com.virtuallab.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LabSession(
  val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String? = null,
  val status: String = "active", // "active", "paused", "stopped"
  val runtimeType: String = "android-container",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
