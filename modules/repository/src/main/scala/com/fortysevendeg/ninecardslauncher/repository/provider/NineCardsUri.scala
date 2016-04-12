package com.fortysevendeg.ninecardslauncher.repository.provider

object NineCardsUri {

  val authorityPart = "com.fortysevendeg.ninecardslauncher2"

  val contentPrefix = "content://"

  val baseUriString = s"$contentPrefix$authorityPart"

  val appUriString = s"$baseUriString/${AppEntity.table}"

  val cardUriString = s"$baseUriString/${CardEntity.table}"

  val collectionUriString = s"$baseUriString/${CollectionEntity.table}"

  val dockAppUriString = s"$baseUriString/${DockAppEntity.table}"

  val momentUriString = s"$baseUriString/${MomentEntity.table}"

  val userUriString = s"$baseUriString/${UserEntity.table}"

}
