package com.sd.demo.compose.refresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
import com.sd.lib.compose.refresh.rememberFRefreshStateBottom
import com.sd.lib.compose.refresh.rememberFRefreshStateTop

class SampleVerticalActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            Surface {
               ContentView()
            }
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

   // 顶部刷新
   val topRefreshState = rememberFRefreshStateTop {
      vm.refresh(10)
   }

   // 底部刷新
   val bottomRefreshState = rememberFRefreshStateBottom {
      vm.loadMore()
   }

   LaunchedEffect(vm) {
      vm.refresh(10)
   }

   Box(
      modifier = modifier
         .fillMaxSize()
         // 顶部
         .nestedScroll(topRefreshState.nestedScrollConnection)
         // 底部
         .nestedScroll(bottomRefreshState.nestedScrollConnection)
   ) {
      ColumnView(uiState.list)

      // 顶部
      FRefreshContainer(
         state = topRefreshState,
         isRefreshing = uiState.isRefreshing,
         modifier = Modifier.align(Alignment.TopCenter),
      )

      // 底部
      FRefreshContainer(
         state = bottomRefreshState,
         isRefreshing = uiState.isLoadingMore,
         modifier = Modifier.align(Alignment.BottomCenter),
      )
   }

   LaunchedEffect(topRefreshState) {
      snapshotFlow { topRefreshState.interactionState }
         .collect {
            logMsg { "top interactionState:$it" }
         }
   }

   LaunchedEffect(bottomRefreshState) {
      snapshotFlow { bottomRefreshState.interactionState }
         .collect {
            logMsg { "bottom interactionState:$it" }
         }
   }
}