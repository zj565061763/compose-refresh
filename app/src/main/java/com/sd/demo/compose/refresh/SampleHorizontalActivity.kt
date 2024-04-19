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
import com.sd.lib.compose.refresh.rememberFRefreshStateLeft
import com.sd.lib.compose.refresh.rememberFRefreshStateRight

class SampleHorizontalActivity : ComponentActivity() {
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

    // 左侧刷新
    val leftRefreshState = rememberFRefreshStateLeft {
        vm.refresh(10)
    }

    // 右侧刷新
    val rightRefreshState = rememberFRefreshStateRight {
        vm.loadMore()
    }

    LaunchedEffect(vm) {
        vm.refresh(10)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 左侧
            .nestedScroll(leftRefreshState.nestedScrollConnection)
            // 右侧
            .nestedScroll(rightRefreshState.nestedScrollConnection)
    ) {
        RowView(uiState.list)

        // 左侧
        FRefreshContainer(
            state = leftRefreshState,
            isRefreshing = uiState.isRefreshing,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // 右侧
        FRefreshContainer(
            state = rightRefreshState,
            isRefreshing = uiState.isLoadingMore,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }

    LaunchedEffect(leftRefreshState) {
        snapshotFlow { leftRefreshState.interactionState }
            .collect {
                logMsg { "left interactionState:$it" }
            }
    }

    LaunchedEffect(rightRefreshState) {
        snapshotFlow { rightRefreshState.interactionState }
            .collect {
                logMsg { "right interactionState:$it" }
            }
    }
}