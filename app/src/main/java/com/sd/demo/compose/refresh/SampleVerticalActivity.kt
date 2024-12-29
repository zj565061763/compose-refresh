package com.sd.demo.compose.refresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose.refresh.theme.AppTheme
import com.sd.lib.compose.refresh.FRefreshContainer
import com.sd.lib.compose.refresh.rememberRefreshStateBottom
import com.sd.lib.compose.refresh.rememberRefreshStateTop

class SampleVerticalActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        ContentView()
      }
    }
  }
}

@Composable
private fun ContentView(
  modifier: Modifier = Modifier,
  vm: PageViewModel = viewModel(),
) {
  val uiState by vm.uiState.collectAsState()

  // top
  val topRefreshState = rememberRefreshStateTop(uiState.isRefreshing) {
    vm.refresh(10)
  }

  // bottom
  val bottomRefreshState = rememberRefreshStateBottom(uiState.isLoadingMore) {
    vm.loadMore()
  }

  LaunchedEffect(vm) {
    vm.refresh(10)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      // top
      .nestedScroll(topRefreshState.nestedScrollConnection)
      // bottom
      .nestedScroll(bottomRefreshState.nestedScrollConnection)
  ) {
    ColumnView(uiState.list)

    // top
    FRefreshContainer(
      state = topRefreshState,
      modifier = Modifier.align(Alignment.TopCenter),
    )

    // bottom
    FRefreshContainer(
      state = bottomRefreshState,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }

  LaunchedEffect(topRefreshState) {
    snapshotFlow { topRefreshState.currentInteraction }
      .collect {
        logMsg { "top currentInteraction:$it" }
      }
  }

  LaunchedEffect(bottomRefreshState) {
    snapshotFlow { bottomRefreshState.currentInteraction }
      .collect {
        logMsg { "bottom currentInteraction:$it" }
      }
  }
}