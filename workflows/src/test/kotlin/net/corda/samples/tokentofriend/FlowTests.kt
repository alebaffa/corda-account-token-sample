package net.corda.samples.tokentofriend

import com.r3.corda.lib.accounts.workflows.flows.OurAccounts
import groovy.util.GroovyTestCase.assertEquals
import net.corda.core.concurrent.CordaFuture
import net.corda.core.utilities.getOrThrow
import net.corda.samples.tokentofriend.flows.*
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var c: StartedMockNode
    private lateinit var d: StartedMockNode
    private lateinit var e: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.corda.lib.ci"),
                    TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                    TestCordapp.findCordapp("net.corda.samples.tokentofriend.contracts"),
                    TestCordapp.findCordapp("net.corda.samples.tokentofriend.flows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
                ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
            )
        )
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()
        d = network.createPartyNode()
        e = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `account is created and shared`() {
        val accountName = "bob"

        a.startFlow(CreateNewAccountFlow(accountName)).runAndGet(network)
        val nodeAccounts = a.startFlow(OurAccounts())
        network.runNetwork()
        assertEquals(accountName, nodeAccounts.get()[0].state.data.name)
    }

    @Test
    fun `Node issues tokens to its own account`() {
        val issuerEmail = "issuer@mail.com"
        val recipientAccount = "Bob"
        // create an account hosted by the current node
        a.startFlow(CreateNewAccountFlow(recipientAccount))
        // create a state
        val uuid = a.startFlow(CreateMyTokenFlow(recipientAccount, "random string")).runAndGet(network)
        // issue fungible tokens of that state
        a.startFlow(IssueTokenFlow(uuid.toString(), 10, recipientAccount)).runAndGet(network)

        // test the amount held of tokens are the value expected
        val tokenBalance = a.startFlow(MyAccountsTokenBalanceFlow(recipientAccount)).runAndGet(network)
        assertEquals(10, tokenBalance)
    }

    @Test
    fun `Account moves tokens to another account in the same hosting node`() {
        val accountOne = "Sally"
        val accountTwo = "Bob"

        a.startFlow(CreateNewAccountFlow(accountOne)).runAndGet(network)
        a.startFlow(CreateNewAccountFlow(accountTwo)).runAndGet(network)

        // create a state
        val uuid = a.startFlow(CreateMyTokenFlow(accountTwo, "random string")).runAndGet(network)
        // issue fungible tokens of that state to account1
        a.startFlow(IssueTokenFlow(uuid.toString(), 10, accountOne)).runAndGet(network)

        // test the amount held of tokens are the value expected
        val tokenBalanceAccountOne = a.startFlow(MyAccountsTokenBalanceFlow(accountOne)).runAndGet(network)
        assertEquals(10, tokenBalanceAccountOne)

        // Sally moves tokens to Bob
        a.startFlow(MoveTokenToAccountFlow(5,uuid, accountOne, accountTwo)).runAndGet(network)

        // test the amount held of tokens are the value expected
        val tokenBalanceAccountOneAfterMove = a.startFlow(MyAccountsTokenBalanceFlow(accountOne)).runAndGet(network)
        assertEquals(5, tokenBalanceAccountOneAfterMove)
        val tokenBalanceAccountTwo = a.startFlow(MyAccountsTokenBalanceFlow(accountTwo)).runAndGet(network)
        assertEquals(5, tokenBalanceAccountTwo)


    }

    private fun <V> CordaFuture<V>.runAndGet(network: MockNetwork): V {
        network.runNetwork()
        return this.getOrThrow()
    }
}