/*
 * Copyright 2022 Stax
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hover.stax.data.accounts

import android.content.Context
import com.hover.sdk.api.ActionApi
import com.hover.sdk.sims.SimInfo
import com.hover.stax.R
import com.hover.stax.data.actions.ActionRepo
import com.hover.stax.data.channel.ChannelRepository
import com.hover.stax.database.models.Account
import com.hover.stax.notifications.PushNotificationTopicsInterface
import com.hover.stax.utils.AnalyticsUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    val accountRepo: AccountRepo,
    private val channelRepository: ChannelRepository,
    val actionRepo: ActionRepo,
    @ApplicationContext val context: Context
) :
    AccountRepository, PushNotificationTopicsInterface {

    override val addedAccounts: Flow<List<Account>>
        get() = accountRepo.getAccounts()

    override suspend fun createAccount(sim: SimInfo): Account {
        var account = Account(generateSimBasedName(sim), generateSimBasedAlias(sim))
        channelRepository.getTelecom(sim.osReportedHni)?.let {
            account = Account(account.userAlias, it, false, sim.subscriptionId)
            accountRepo.insert(account)
        }
        logChoice(account)
        ActionApi.scheduleActionConfigUpdate(account.countryAlpha2, 24, context)
        return account
    }

    private fun generateSimBasedName(sim: SimInfo): String {
        return (
            sim.operatorName ?: sim.networkOperatorName
                ?: ""
            ) + "-" + sim.subscriptionId.toString()
    }

    private fun generateSimBasedAlias(sim: SimInfo): String {
        return sim.operatorName ?: sim.networkOperatorName ?: "Unknown"
    }

    override suspend fun getAccountBySim(subscriptionId: Int): Account? {
        return accountRepo.getAccountBySim(subscriptionId)
    }

    private fun logChoice(account: Account) {
        joinChannelGroup(account.channelId, context)
        val args = JSONObject()

        try {
            args.put(context.getString(R.string.added_channel_id), account.channelId)
        } catch (ignored: Exception) {
        }

        AnalyticsUtil.logAnalyticsEvent(context.getString(R.string.new_sim_channel), args, context)
    }
}