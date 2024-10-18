package com.sd.demo.compose.refresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose.refresh.theme.AppTheme
import com.sd.lib.compose.refresh.FRefreshContainer
import com.sd.lib.compose.refresh.rememberFRefreshStateEnd
import com.sd.lib.compose.refresh.rememberFRefreshStateStart

class SampleHorizontalActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            Surface {
               CompositionLocalProvider(
                  LocalLayoutDirection provides LayoutDirection.Rtl
               ) {
                  ContentView()
               }
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

   // start
   val startRefreshState = rememberFRefreshStateStart { vm.refresh(10) }
   // end
   val endRefreshState = rememberFRefreshStateEnd { vm.loadMore() }

   LaunchedEffect(vm) {
      vm.refresh(10)
   }

   Box(
      modifier = modifier
         .fillMaxSize()
         // start
         .nestedScroll(startRefreshState.nestedScrollConnection)
         // end
         .nestedScroll(endRefreshState.nestedScrollConnection)
   ) {
      RowView(uiState.list)

      // start
      FRefreshContainer(
         state = startRefreshState,
         isRefreshing = uiState.isRefreshing,
         modifier = Modifier.align(Alignment.CenterStart),
      )

      // end
      FRefreshContainer(
         state = endRefreshState,
         isRefreshing = uiState.isLoadingMore,
         modifier = Modifier.align(Alignment.CenterEnd),
      )
   }

   LaunchedEffect(startRefreshState) {
      snapshotFlow { startRefreshState.interactionState }
         .collect {
            logMsg { "start interactionState:$it" }
         }
   }

   LaunchedEffect(endRefreshState) {
      snapshotFlow { endRefreshState.interactionState }
         .collect {
            logMsg { "end interactionState:$it" }
         }
   }
}