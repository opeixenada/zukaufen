package zukaufen.auth


trait UserStoreClient {

  def isUserLoggedIn(uid: String, token: String): Boolean

}


trait UserStoreClientComponent {

  def userStoreClient: UserStoreClient = UserStoreClientMock

  object UserStoreClientMock extends UserStoreClient {
    def isUserLoggedIn(uid: String, token: String): Boolean = true
  }

}