# Animated LazyColumn/LazyRow
 POC of how you can animate LazyColumn/LazyRow insertions/deletions/moving
 
 Note, this is not production ready or a library, but just a POC on a potential workaround until official support for `LazyColumn` animations is available, follow issue tracker:
 https://issuetracker.google.com/issues/150812265
 
 DEMO:

https://user-images.githubusercontent.com/22520376/135706701-1dc1ba4d-3822-476e-b10b-5e2060a00678.mp4

https://user-images.githubusercontent.com/22520376/135711897-8bd580f5-9f9c-4768-b6ee-54f7c7763f7a.mp4


Example usage:

```koltin
data class MainItem(
    val id: String,
    val text: String
)

val items = List(10) { MainItem(UUID.randomUUID().toString(), UUID.randomUUID().toString()) }
val state = rememberLazyListState()

AnimatedLazyColumn(
   state = state,
   items = items.map {
       AnimatedLazyListItem(key = it.id, value = it.text) {
           TextItem(viewModel, it)
       }
   }
)

AnimatedLazyRow(
   state = state,
   items = items.map {
       AnimatedLazyListItem(key = it.id, value = it.text) {
           TextItem(viewModel, it)
       }
   }
)

```
