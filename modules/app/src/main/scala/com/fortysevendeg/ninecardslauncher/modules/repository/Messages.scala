package com.fortysevendeg.ninecardslauncher.modules.repository

import com.fortysevendeg.ninecardslauncher.repository.model.GeoInfo

case class GetCollectionsRequest()

case class GetCollectionsResponse(collections: Seq[Collection])

case class GetCacheCategoryRequest()

case class GetCacheCategoryResponse(cacheCategory: Seq[CacheCategory])

case class InsertGeoInfoRequest(
    constrain: String,
    occurrence: String,
    wifi: String,
    latitude: Double,
    longitude: Double,
    system: Boolean)

case class InsertGeoInfoResponse(
    geoInfo: Option[GeoInfo])

case class InsertCollectionRequest(
    position: Int,
    name: String,
    `type`: String,
    icon: String,
    themedColorIndex: Int,
    appsCategory: Option[String] = None,
    constrains: Option[String] = None,
    originalSharedCollectionId: Option[String] = None,
    sharedCollectionId: Option[String] = None,
    sharedCollectionSubscribed: Boolean,
    cards : Seq[CardItem])

case class CardItem(
    position: Int,
    micros: Int = 0,
    term: String,
    packageName: Option[String],
    `type`: String,
    intent: String,
    imagePath: String,
    starRating: Option[Double] = None,
    numDownloads: Option[String] = None,
    notification: Option[String] = None)

case class InsertCollectionResponse(
    success: Boolean)