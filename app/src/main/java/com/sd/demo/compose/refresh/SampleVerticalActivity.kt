package com.sd.demo.compose.refresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    val topRefreshState = rememberFRefreshStateTop {
        vm.refresh(20)
    }

    LaunchedEffect(vm) {
        vm.refresh(20)
    }

    Column(modifier = modifier) {

        Button(onClick = { vm.refresh(10) }) {
            Text(text = "refresh")
        }

        Box(
            modifier = modifier
                .weight(1f)
                .nestedScroll(topRefreshState.nestedScrollConnection)
        ) {
            ColumnView(
                list = uiState.list,
                modifier = Modifier.fillMaxSize(),
            )

            FRefreshContainer(
                state = topRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    LaunchedEffect(topRefreshState) {
        snapshotFlow { topRefreshState.interactionState }
            .collect {
                logMsg { "interactionState:$it" }
            }
    }
}