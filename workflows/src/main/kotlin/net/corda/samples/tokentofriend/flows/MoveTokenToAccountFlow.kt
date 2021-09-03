package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.node.services.queryBy


@InitiatingFlow
@StartableByRPC
class MoveTokenToAccountFlow(private val quantity: Long,
                       private val uuid: UniqueIdentifier,
                       private val sender: String,
                       private val receiver: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        // Corda node retrieves the keys of the sender account
        val senderStateAndRef = accountService.accountInfo(sender).single().state.data
        val senderAccount = retrieveAccountKey(senderStateAndRef)

        // Corda node retrieves the keys of the receiver account
        val receiverStateAndRef = accountService.accountInfo(receiver).single().state.data
        val receiverAccount = retrieveAccountKey(receiverStateAndRef)

        when {
            senderStateAndRef.host != ourIdentity -> {
                throw IllegalArgumentException("Sender account must be hosted on the current node")
            }
            else -> {
                // Retrieve the state from the vault.
                val statePointer: TokenPointer<CustomTokenState> = queryStatePointer(serviceHub, uuid)
                val amount: Amount<TokenType> = Amount(quantity, statePointer)

                // Prepare a query criteria to move only the tokens belonging to the sender account.
                // Using `withExternalIds` helps to filter by account's UUID
                val tokenQueryCriteria =
                    QueryCriteria.VaultQueryCriteria().withExternalIds(listOf(senderStateAndRef.identifier.id))

                // Build the TransactionBuilder
                val transactionBuilder = TransactionBuilder(getPreferredNotary(serviceHub))

                // Token SDK utility that fill the TransactionBuilder with the correct input states and output states.
                addMoveFungibleTokens(
                    transactionBuilder,
                    serviceHub,
                    amount,
                    receiverAccount,
                    senderAccount,
                    tokenQueryCriteria
                )
                transactionBuilder.verify(serviceHub)

                // sign the transaction with the Corda node public key and the sender account public key
                val partiallySigned = serviceHub.signInitialTransaction(
                    transactionBuilder,
                    listOf(senderAccount.owningKey)
                )
                // create session with the receiver account
                val counterPartySessions =
                    if (receiverStateAndRef.host == ourIdentity) emptyList() else listOf(initiateFlow(receiverAccount))

                // collect signature of the receiver account
                val fullySigned =
                    if (counterPartySessions.isEmpty())
                        serviceHub.addSignature(partiallySigned, receiverAccount.owningKey)
                    else
                        subFlow(CollectSignaturesFlow(partiallySigned, counterPartySessions))

                // store the transaction in the ledger of sender and receiver
                return subFlow(FinalityFlow(fullySigned, counterPartySessions))
            }
        }
    }

    fun queryState(serviceHub: ServiceHub, uuid: UniqueIdentifier): StateAndRef<CustomTokenState> {
        val stateAndRefs: List<StateAndRef<CustomTokenState>> = serviceHub.vaultService.queryBy<CustomTokenState>()
            .states.filter { it.state.data.linearId == uuid }
        // Match the query result with the symbol. If no results match, throw exception
        return stateAndRefs.stream()
            .findAny()
            .orElseThrow { IllegalArgumentException("State not found in vault.") }
    }

    fun queryStatePointer(serviceHub: ServiceHub, uuid: UniqueIdentifier): TokenPointer<CustomTokenState> {
        val (state) = queryState(serviceHub, uuid)
        return state.data.toPointer(CustomTokenState::class.java)
    }

    @Suspendable
    private fun retrieveAccountKey (accountInfo: AccountInfo): AnonymousParty {
        val accountKeys = accountService.accountKeys(accountInfo.identifier.id)
        return if (accountKeys.isEmpty()){
            subFlow(RequestKeyForAccount(accountInfo))
        }
        else{
            AnonymousParty(accountKeys.first())
        }
    }
}