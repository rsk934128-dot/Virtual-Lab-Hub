package com.virtuallab.app.data.repository

import com.google.firebase.firestore.FirebaseFirestoreException
import com.virtuallab.app.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LabRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfile_authenticatedUser_createsDocument() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = LabRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureUserProfile("Alice Developer", ALICE_EMAIL)
    }
    assertTrue("User profile creation should succeed", result.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserProfile().first()
    }
    assertNotNull(profile)
    assertEquals(uid, profile?.userId)
    assertEquals("Alice Developer", profile?.displayName)
  }

  @Test
  fun createAndObserveSession_authenticatedOwner_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = LabRepository(firestore)

    val sessionId = "session_${UUID.randomUUID().toString().take(8)}"
    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createSession(sessionId, "Kernel Lab Test", "Sandbox container test", "android-container")
    }
    assertTrue("Session creation should succeed", createResult.isSuccess)

    val sessions = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeSessions().first { list -> list.any { it.id == sessionId } }
    }
    assertTrue(sessions.any { it.id == sessionId && it.title == "Kernel Lab Test" })
  }

  @Test
  fun observeSessions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = LabRepository(firestore)

    var caught = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeSessions().first()
      }
    } catch (e: Exception) {
      caught = true
      // Unauthenticated fails at requireUserId() or via Firestore listener error
      val isIllegalState = e is IllegalStateException
      val isFirestorePermDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      assertTrue(isIllegalState || isFirestorePermDenied)
    }
    assertTrue("Should fail for unauthenticated caller", caught)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@lab.test"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
