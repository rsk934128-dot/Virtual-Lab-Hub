package com.virtuallab.app.data.repository

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.virtuallab.app.R
import com.virtuallab.app.data.model.LabSession
import com.virtuallab.app.data.model.UserProfile
import com.virtuallab.app.data.util.OperationType
import com.virtuallab.app.data.util.handleFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LabRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureUserProfile(displayName: String, email: String?): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = buildMap<String, Any> {
          put("userId", uid)
          put("displayName", displayName.ifBlank { "Developer" })
          if (!email.isNullOrBlank()) put("email", email)
          put("createdAt", FieldValue.serverTimestamp())
        }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            val profile = snapshot.toObject(UserProfile::class.java)
            profile?.copy(userId = profile.userId.ifBlank { snapshot.id })
          } else null
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  fun observeSessions(): Flow<List<LabSession>> = flow {
    val uid = requireUserId()
    val collectionRef = db.collection("users").document(uid).collection("sessions")
    emitAll(
      collectionRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            try {
              doc.toObject(LabSession::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
              // Fallback for documents that contain a stored 'id' field in Firestore
              val data = doc.data ?: return@mapNotNull null
              LabSession(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                description = data["description"] as? String,
                status = data["status"] as? String ?: "active",
                runtimeType = data["runtimeType"] as? String ?: "android-container",
                createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
              )
            }
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
          }
          throw error
        }
    )
  }

  suspend fun createSession(
    sessionId: String,
    title: String,
    description: String?,
    runtimeType: String
  ): Result<String> {
    val uid = requireUserId()
    val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
    return try {
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("title", title)
        if (!description.isNullOrBlank()) put("description", description)
        put("status", "active")
        put("runtimeType", runtimeType)
        put("createdAt", FieldValue.serverTimestamp())
      }
      sessionRef.set(payload).await()
      Result.success(sessionId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, sessionRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateSessionStatus(sessionId: String, newStatus: String): Result<Unit> {
    val uid = requireUserId()
    val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
    return try {
      val updateData = mapOf<String, Any>(
        "status" to newStatus,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      sessionRef.update(updateData).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, sessionRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSession(sessionId: String): Result<Unit> {
    val uid = requireUserId()
    val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
    return try {
      sessionRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, sessionRef.path)
      Result.failure(e)
    }
  }
}
