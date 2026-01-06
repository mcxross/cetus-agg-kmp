# KMP Cetus Aggregator

A **Kotlin Multiplatform (KMP)** SDK that makes interacting with the [Cetus Aggregator](https://www.cetus.zone/) on Sui **insanely simple**.

Built on top of [ksui](https://github.com/mcxross/ksui), this SDK handles all the complex routing, gas management, and transaction construction for you. It supports **JVM**, **Android**, **iOS** and **macOS**.

## Why use this?

*   **Insanely Simple API**: Perform swaps in just a few lines of code.
*   **Smart Routing**: Automatically finds the best route for your swap.
*   **Type-Safe**: Built with a Kotlin DSL for safe and readable configuration.
*   **Multiplatform**: Write your swap logic once, run it everywhere.

## Setup

Add the SDK to your `commonMain` dependencies in `build.gradle.kts`:

```kotlin
implementation("xyz.mcxross:aggregator:0.1.0") // Replace with actual version/path
implementation("xyz.mcxross.ksui:ksui:2.2.0")
```

## Usage

### 1. Initialize

First, set up your Sui client (using `ksui`) and the Aggregator.

```kotlin
import xyz.mcxross.ksui.model.SuiConfig
import xyz.mcxross.ksui.model.SuiSettings
import xyz.mcxross.ksui.model.Network
import xyz.mcxross.agg.cetus.dsl.Aggregator

// Initialize ksui (Mainnet)
val sui = Sui(SuiConfig(SuiSettings(network = Network.MAINNET)))

// Initialize the Aggregator
val aggregator = Aggregator()
```

### 2. Perform a Swap

Use the simple DSL to build your swap transaction.

```kotlin
import xyz.mcxross.agg.cetus.dsl.*

// Build the transaction
val tx = aggregator.swap {
    from = Tokens.SUI
    to = Tokens.CETUS
    amount = 1.sui        // 1 SUI (automatically converts to mist)
    slippage = 0.5.percent // 0.5% slippage
    recipient = "0xYourAddress..."
}

// Sign and execute
val signer = Account.import(YOUR_PRIV_KEY)
val response = sui.signAndExecuteTransactionBlock(signer, tx)

println("Swap executed: ${response.digest}")
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.