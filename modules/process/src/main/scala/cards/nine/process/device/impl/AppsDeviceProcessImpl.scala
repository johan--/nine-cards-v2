/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cards.nine.process.device.impl

import cards.nine.commons.NineCardExtensions._
import cards.nine.commons.contexts.ContextSupport
import cards.nine.commons.services.TaskService
import cards.nine.commons.services.TaskService._
import cards.nine.models.Application.ApplicationDataOps
import cards.nine.models.types.{Misc, _}
import cards.nine.models.{Application, ApplicationData}
import cards.nine.process.device._
import cards.nine.process.utils.ApiUtils
import cards.nine.repository.model.{App => RepositoryApp}
import cards.nine.services.image._
import cards.nine.services.persistence.ImplicitsPersistenceServiceExceptions
import cards.nine.services.persistence.conversions.AppConversions

trait AppsDeviceProcessImpl extends DeviceProcess with AppConversions {

  self: DeviceProcessDependencies
    with ImplicitsDeviceException
    with ImplicitsImageExceptions
    with ImplicitsPersistenceServiceExceptions =>

  val apiUtils = new ApiUtils(persistenceServices)

  def getSavedApps(orderBy: GetAppOrder)(implicit context: ContextSupport) =
    (for {
      apps <- persistenceServices.fetchApps(toFetchAppOrder(orderBy), orderBy.ascending)
    } yield apps map (_.toData)).resolve[AppException]

  def getIterableApps(orderBy: GetAppOrder)(implicit context: ContextSupport) =
    (for {
      iter <- persistenceServices.fetchIterableApps(toFetchAppOrder(orderBy), orderBy.ascending)
    } yield iter).resolve[AppException]

  def getIterableAppsByCategory(category: String)(implicit context: ContextSupport) =
    (for {
      iter <- persistenceServices.fetchIterableAppsByCategory(
        category,
        OrderByName,
        ascending = true)
    } yield iter).resolve[AppException]

  def getTermCountersForApps(orderBy: GetAppOrder)(implicit context: ContextSupport) =
    (for {
      counters <- orderBy match {
        case GetByName     => persistenceServices.fetchAlphabeticalAppsCounter
        case GetByCategory => persistenceServices.fetchCategorizedAppsCounter
        case _             => persistenceServices.fetchInstallationDateAppsCounter
      }
    } yield counters).resolve[AppException]

  def getIterableAppsByKeyWord(keyword: String, orderBy: GetAppOrder)(
      implicit context: ContextSupport) =
    (for {
      iter <- persistenceServices.fetchIterableAppsByKeyword(
        keyword,
        toFetchAppOrder(orderBy),
        orderBy.ascending)
    } yield iter).resolve[AppException]

  def synchronizeInstalledApps(implicit context: ContextSupport) = {

    def deleteAndFilter(
        existingApps: Seq[Application],
        idsToRemove: Seq[Int]): TaskService[Seq[Application]] =
      if (idsToRemove.nonEmpty) {
        for {
          _ <- persistenceServices.deleteAppsByIds(idsToRemove)
          filteredApps <- TaskService.right(
            existingApps.filterNot(app => idsToRemove.contains(app.id)))
        } yield filteredApps
      } else TaskService.right(existingApps)

    def fixDuplicatedPackages: TaskService[Seq[Application]] = {
      for {
        allApps <- persistenceServices.fetchApps(OrderByInstallDate, ascending = false)
        (miscAps, categorizedApps) = allApps.partition(_.category == Misc)
        duplicatedIds = categorizedApps
          .groupBy(app => s"${app.packageName}:${app.className}")
          .flatMap {
            case (packageName, seq) => seq.tail.map(_.id)
          }
          .toSeq
        miscIds = miscAps.map(_.id)
        filteredApps <- deleteAndFilter(
          existingApps = allApps,
          idsToRemove = (duplicatedIds ++ miscIds).distinct)
      } yield filteredApps
    }

    def categorizeAndSaveNewApps(filteredApps: Seq[ApplicationData]): TaskService[Unit] =
      if (filteredApps.nonEmpty) {
        for {
          requestConfig <- apiUtils.getRequestConfig
          categorizedPackages <- apiServices
            .googlePlayPackages(filteredApps map (_.packageName))(requestConfig)
            .resolveLeftTo(Seq.empty)
          apps = filteredApps map { app =>
            val category = categorizedPackages find (_.packageName == app.packageName) flatMap (_.category) getOrElse Misc
            app.copy(category = category)
          }
          _ <- persistenceServices.addApps(apps)
        } yield ()
      } else TaskService.empty

    (for {
      installedApps <- appsServices.getInstalledApplications
      dbApps        <- fixDuplicatedPackages
      filteredApps <- TaskService.right(
        installedApps.filterNot(app => dbApps.exists(_.packageName == app.packageName)))
      _ <- categorizeAndSaveNewApps(filteredApps)
    } yield ()).resolve[AppException]
  }

  def saveApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      application      <- appsServices.getApplication(packageName)
      appCategory      <- getAppCategory(packageName)
      applicationAdded <- persistenceServices.addApp(application.copy(category = appCategory))
    } yield applicationAdded.toData).resolve[AppException]

  def deleteApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      _ <- persistenceServices.deleteAppByPackage(packageName)
    } yield ()).resolve[AppException]

  def updateApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      app <- appsServices.getApplication(packageName)
      appPersistence <- persistenceServices
        .findAppByPackage(packageName)
        .resolveOption(s"Can't find the application with package name $packageName")
      appCategory <- getAppCategory(packageName)
      _           <- persistenceServices.updateApp(app.copy(category = appCategory).toApp(appPersistence.id))
    } yield ()).resolve[AppException]

  private[this] def toFetchAppOrder(orderBy: GetAppOrder): FetchAppOrder = orderBy match {
    case GetByName(_)        => OrderByName
    case GetByInstallDate(_) => OrderByInstallDate
    case GetByCategory(_)    => OrderByCategory
  }

  private[this] def getAppCategory(packageName: String)(implicit context: ContextSupport) =
    for {
      requestConfig <- apiUtils.getRequestConfig
      appCategory <- apiServices
        .googlePlayPackage(packageName)(requestConfig)
        .map(_.category)
        .resolveLeftTo(None)
    } yield appCategory getOrElse Misc

}
