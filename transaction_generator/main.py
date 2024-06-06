import argparse

from transaction_simulator import TransactionSimulator


def main():
    parser = argparse.ArgumentParser(description="Simulation parameters.")
    parser.add_argument(
        "--sim_speed",
        type=int,
        default=1_000,
        help="Simulation speed in transactions per second (default: 1,000, max: 50,000)",
    )
    parser.add_argument(
        "--anomaly_chance", type=float, default=3, help="Chance of anomaly occurrence in percents (default: 3)"
    )
    parser.add_argument("--card_limit", type=int, default=10_000, help="Number of cards to simulate (default: 10,000)")
    parser.add_argument("--user_limit", type=int, default=1_000, help="Number of users to simulate (default: 1,000)")
    parser.add_argument(
        "--filename", type=str, default="cards.json", help="Filename for generated cards (default: cards.json)"
    )
    parser.add_argument("--generate", action="store_true", help="Generate new cards")
    args = parser.parse_args()

    print(f"Simulation speed: {args.sim_speed}")
    print(f"Anomaly chance: {args.anomaly_chance}%")

    simulator = TransactionSimulator(
        sim_speed=args.sim_speed,
        anomaly_chance=args.anomaly_chance,
        card_limit=args.card_limit,
        user_limit=args.user_limit,
        filename=args.filename,
        generate=args.generate,
    )
    simulator.run()


if __name__ == "__main__":
    main()
