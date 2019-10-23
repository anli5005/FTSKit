# FTSKit
Bringing algorithmic trading to the [Financial Trading System Market Simulator.](http://www.ftsmodules.com)

Uses the very bad [web interface](http://www.ftsrealtime.com) web socket API. Serves as an alternative to the very bad [Python Windows interface](http://www.ftsmodules.com/public/modules/ftsrt/python/algorithmictrading_python.pdf).

Made by [Anthony Li](https://anli.dev).

## Example Usage
The API is very much still in development, but here's a taste of what the API is like to use:

```kotlin
fun main() {
    runBlocking {
        val quantity = 1000
    
        // Start the trading session.
        val session = TradingSession("ws://ftsrealtime.com/ws.ashx", "FTS 1000 Stocks Case", username, password)
        session.start()
    
        // Fetch Netflix's ticker.
        session.tickerDatabase.refreshDatabase()
        val ticker = session.tickerDatabase.infoForSecurityName("Netflix, Inc.")!!.ticker
    
        // Short sell Netflix for 60 seconds.
        session.place(ShortSellOrder(ticker, quantity))
        delay(60 * 1000)
    
        // Clean up.
        session.place(ShortCoverOrder(ticker, quantity))
        session.waitForIdle()
        println("Done!")
    }
}
```