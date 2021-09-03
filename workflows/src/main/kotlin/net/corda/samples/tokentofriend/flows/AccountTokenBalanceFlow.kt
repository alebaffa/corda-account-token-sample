package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import java.util.*

@StartableByRPC
@CordaSerializable
class MyAccountsTokenBalanceFlow(private val accountName: String): FlowLogic<Long>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Long {
        val accountList = accountService.accountInfo(accountName)
        if (accountList.equals(null) || accountList.isEmpty())
            throw IllegalArgumentException("ERROR: there are no accounts")

        val accountIdentifier = accountList[0].state.data.identifier
        if(accountIdentifier.equals(null) || accountService.accountInfo(accountName).isEmpty())
            throw IllegalArgumentException("ERROR: there are no accounts with name '$accountName`")

        val criteria = QueryCriteria.VaultQueryCriteria().withExternalIds(listOf(accountIdentifier.id))

        return serviceHub.vaultService.queryBy<FungibleToken>(criteria).states[0].state.data.amount.quantity
    }
}