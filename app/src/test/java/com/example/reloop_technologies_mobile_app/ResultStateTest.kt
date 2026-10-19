package com.example.reloop_technologies_mobile_app

import com.example.reloop_technologies_mobile_app.backend.ResultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultStateTest {

    @Test
    fun testResultStateSuccess() {
        val successState: ResultState<String> = ResultState.Success("Data loaded successfully")
        assertTrue(successState is ResultState.Success)
        assertEquals("Data loaded successfully", (successState as ResultState.Success).data)
    }

    @Test
    fun testResultStateError() {
        val errorState: ResultState<String> = ResultState.Error(message = "Network failure")
        assertTrue(errorState is ResultState.Error)
        assertEquals("Network failure", (errorState as ResultState.Error).message)
    }

    @Test
    fun testResultStateLoading() {
        val loadingState: ResultState<String> = ResultState.Loading
        assertTrue(loadingState is ResultState.Loading)
    }
}
