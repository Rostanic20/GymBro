package hr.rostanic20.gymbro.ui.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.rostanic20.gymbro.domain.model.Food
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.ui.launchReporting
import hr.rostanic20.gymbro.ui.stateInWhileSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow

class FoodsViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    val foods: StateFlow<List<Food>?> = foodRepository.foods().stateInWhileSubscribed(viewModelScope, null)

    fun toggleFavourite(food: Food) {
        launchReporting(_messages) { foodRepository.setFavourite(food.id, !food.isFavourite) }
    }
}

data class FoodEditState(val food: Food?, val loaded: Boolean)

sealed interface FoodEditEvent {
    data object Saved : FoodEditEvent
}

class FoodEditViewModel(
    private val foodId: Long?,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    private val _events = Channel<FoodEditEvent>(Channel.BUFFERED)
    val events: Flow<FoodEditEvent> = _events.receiveAsFlow()

    val state: StateFlow<FoodEditState> =
        flow { emit(FoodEditState(food = foodId?.let { foodRepository.food(it) }, loaded = true)) }
            .stateInWhileSubscribed(viewModelScope, FoodEditState(food = null, loaded = false))

    fun save(draft: FoodDraft) {
        launchReporting(_messages) {
            foodRepository.saveFood(draft)
            _events.send(FoodEditEvent.Saved)
        }
    }
}
