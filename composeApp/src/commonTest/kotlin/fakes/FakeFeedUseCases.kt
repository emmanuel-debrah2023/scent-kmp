package fakes

import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.usecase.GetFeedUseCase
import org.scent.project.domain.usecase.LikePostUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft

private class FakePostRepository : PostRepository {
    override suspend fun getFeed(
        cursor: String?,
        limit: Int,
    ): Result<FeedPage> = AppError.Unknown().asLeft()

    override suspend fun likePost(postId: String): Result<LikeResult> = AppError.Unknown().asLeft()

    override suspend fun createPost(params: CreatePostParams): Result<Post> = AppError.Unknown().asLeft()
}

class FakeGetFeedUseCase : GetFeedUseCase(FakePostRepository()) {
    var result: Result<FeedPage>? = null
    var lastCursor: String? = null
    var onInvoke: (() -> Unit)? = null

    override suspend fun invoke(
        cursor: String?,
        limit: Int,
    ): Result<FeedPage> {
        lastCursor = cursor
        onInvoke?.invoke()
        return result ?: error("FakeGetFeedUseCase.result not set")
    }
}

class FakeLikePostUseCase : LikePostUseCase(FakePostRepository()) {
    var result: Result<LikeResult>? = null
    var lastPostId: String? = null

    override suspend fun invoke(postId: String): Result<LikeResult> {
        lastPostId = postId
        return result ?: error("FakeLikePostUseCase.result not set")
    }
}
