# Wykrywanie anomalii w symulowanych tranzakcjach kartami płatniczymi
*Maciej Kowalczyk*

# Sposób uzycia
1. Uruchomić dockerowe obrazy programów `flink` i `kafka` poleceniem `docker compose up -d`
2. Skompilować aplikację do detekcji anomalii (z uzyciem `maven`) i wgrać skompilowany job do `flink job manager` dostępnego pod adresem `http://127.0.0.1:8081`
3. Przejść do katalogu `data_visualization` i uruchomić aplikację do wizualizacji poleceniem `python3 card_visualizer.py`
4. Przejść do katalogu `transaction_generator` i uruchomić generator poleceniem `python3 main.py [params]`

Zaleca się uzycie `venv`, pliki `requirements.txt` znajdują się w poszczególnych katalogach.

# Moduły

## Generator tranzakcji
Generator tranzakcji został napisany w języku `python`. 

### Dostępne parametry
1. `--sim_speed`
   - Typ: `int`
   - Domyślna wartość: `1_000`
   - Opis: Prędkość symulacji w transakcjach na sekundę (domyślnie: 1,000, maksymalnie: 50,000).

2. `--anomaly_chance`
   - Typ: `float`
   - Domyślna wartość: `3`
   - Opis: Szansa na wystąpienie anomalii w procentach (domyślnie: 3).

3. `--card_limit`
   - Typ: `int`
   - Domyślna wartość: `10_000`
   - Opis: Liczba kart do zasymulowania (domyślnie: 10,000).

4. `--user_limit`
   - Typ: `int`
   - Domyślna wartość: `1_000`
   - Opis: Liczba użytkowników do zasymulowania (domyślnie: 1,000).

5. `--filename`
   - Typ: `str`
   - Domyślna wartość: `"cards.json"`
   - Opis: Nazwa pliku dla wygenerowanych kart (domyślnie: `cards.json`).

6. `--generate`
   - Typ: `bool`
   - Opis: Flaga informująca o generowaniu nowych kart. Jeśli ustawiona, program będzie generował nowe karty.

Dane poprawnych transakcji generowane są tak, aby miały sens statystyczny, to znaczy, dla danej karty losujemy dane transakcji z uzyciem rozkładu normalnego.

### Generowane anomalii
1. **Value anomaly** (Anomalia wartości):
   - Sposób realizacji:
     ```python
        value *= 5
        if value > card.balance:
            value = card.balance
        card.balance -= value
     ```
   - Opis: Ta anomalia polega na zwiększeniu wartości transakcji pięciokrotnie. Jeśli nowa wartość transakcji przekracza aktualny stan konta karty, transakcja zostaje ograniczona do dostępnego salda.

2. **Location anomaly** (Anomalia lokalizacji):
   - Sposób realizacji:
     ```python
        longitude += random.uniform(-90, 90)
        latitude += random.uniform(-180, 180)
     ```
   - Opis: Ta anomalia polega na losowej zmianie współrzędnych geograficznych (długości i szerokości geograficznej). Może symulować sytuacje, w których karta jest używana w niestandardowym miejscu.

3. **Time anomaly** (Anomalia czasowa):
   - Sposób realizacji:
     ```python
        for _ in range(5):
            if value > card.balance:
                raise StopIteration
            card.balance -= value
            transaction_time = int(card.last_transaction_time + abs(random.gauss(60, 30)))
     ```
   - Opis: Ta anomalia dotyczy czasu wykonania transakcji. Generowane jest 5 transakcji w małym interwale, symulując nieprawidłowości w czasie realizacji transakcji.

Wystąpienie wszystkich anomalii jest jednakowo prawdopodobne, a za częstotliwość ich występowania odpowiada parametr `anomaly_chance`

### Struktura generowanych danych

1. **transaction_id**
   - Typ: `string`
   - Opis: Unikalny identyfikator transakcji.

2. **user_id**
   - Typ: `string`
   - Opis: Unikalny identyfikator użytkownika powiązanego z kartą.

3. **card_num**
   - Typ: `string`
   - Opis: Numer karty, na której wykonano transakcję.

4. **value**
   - Typ: `float`
   - Opis: Wartość transakcji.

5. **timestamp**
   - Typ: `int`
   - Opis: Znacznik czasu, kiedy transakcja została wykonana.

6. **location**
   - Typ: `dictionary`
     - `latitude`: `float`
     - `longitude`: `float`
   - Opis: Słownik zawierający współrzędne geograficzne (szerokość i długość geograficzną) miejsca, w którym wykonano transakcję.

7. **balance**
   - Typ: `float`
   - Opis: Aktualny stan konta karty po wykonaniu transakcji.

Przykładowa tranzakcja w formacie JSON:
```json
{
  "transaction_id": "67d3f7c6-1d4f-40c0-b65d-62353c677737",
  "user_id": "c0043973-e1f1-468e-aa29-dab818a936b5",
  "card_num": "393394487",
  "value": 100.50,
  "timestamp": "2024-06-13T12:34:56Z",
  "location": {
    "latitude": 52.2297,
    "longitude": 21.0122
  },
  "balance": 950.75
}
```

Transakcje są wysyłane na topic kafki `CreditCardTransactions`.

## Aplikacja do detekcji anomalii
Aplikację napisano w języku `java` z uzyciem `flink`. Anomalie wykrywane są za pomocą 3 detektorów - wartości, lokalizacji i czasu:
```java
DataStream<Alert> value_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
    .keyBy(Transaction::getCardNumber)
    .process(new ValueAnomalyDetector());

DataStream<Alert> location_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
    .keyBy(Transaction::getCardNumber)
    .process(new LocationAnomalyDetector());

DataStream<Alert> interval_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
    .keyBy(Transaction::getCardNumber)
    .process(new TimeAnomalyDetector());
```

Detektory do detekcji anomalii wykorzystują parametr zScore, który mierzy, jak daleko dana wartość jest od średniej w kontekście odchylenia standardowego

### Sposób działania
1. **Obliczanie zScore**
    - zScore oblicza się jako `(nowa wartość - średnia) / odchylenie standardowe`.
    - zScore wskazuje, jak wiele odchyleń standardowych nowa wartość jest oddalona od średniej.

2. **Wykrywanie Anomalii**
    - Jeśli zScore przekracza 3 lub jest mniejszy niż -3 (co oznacza, że wartość jest daleko od średniej) i liczba transakcji przekracza 10, detektor generuje alert o anomalii.

3. **Aktualizacja Statystyk**:
    - Aktualizacja liczby transakcji, średniej i sumy wariancji za pomocą algorytmu Welforda.
    ```java
    private void updateStatistics(double newValue) throws IOException {
        numberOfTransactions.update(numberOfTransactions.value() + 1);
        double old_delta = newValue - meanState.value();
        meanState.update(meanState.value() + old_delta / numberOfTransactions.value());
        double new_delta = newValue - meanState.value();
        varianceSumState.update(varianceSumState.value() + old_delta * new_delta);
    }
    ```

4. **Przykład**:
    - Transakcja: $100
    - Średnia: $50
    - Odchylenie standardowe: $10
    - zScore = (100 - 50) / 10 = 5

    zScore = 5 oznacza, że transakcja $100 jest 5 odchyleń standardowych powyżej średniej, co jest nietypowe i zostanie oznaczone jako anomalia.

Wszystkie anomalie wysyłane są na topic kafki `TransactionAlerts`.


## Aplikacja do wizualizacji danych
Aplikacja do wizualizacji danych napisana została w języku `python` z wykorzystaniem biblioteki `dash`. Jest to prosta aplikacja webowa umozliwiająca uzytkownikowi wybór karty, z której dane tranzakcji mają być wyświetlane. Dane odczytywane są w osobnych wątkach z dwóch topiców kafki - `CreditCardTransactions` i `TransactionAlerts`.Po wyborze karty wyświetlane są:
- wykres wartości ostatnich tranzakcji
- mapa lokalizacji tranzakcji
- wykres częstotliwości tranzakcji
- lista anomalii wykrytych dla danej karty

Buforowane jest 100 ostatnich wartości.


# Przykład działania
- Generator anomali uruchomiony dla domyślnych parametrów, na konsole drukowane są informacje o wygenerowaniu anomalii
    ![](<img/Screenshot 2024-06-13 at 15.09.08.png>)
- Dane wysyłane są na topic kafki `CreditCardTransactions`
    ![](<img/Screenshot 2024-06-13 at 15.07.01.png>)
- Dane filtrowane są z wykorzystaniem aplikacji flinkowej
    ![](<img/Screenshot 2024-06-13 at 15.06.25.png>)
- Anomalie wysyłane na oddzielny topic `TransactionAlerts`
    ![](<img/Screenshot 2024-06-13 at 15.07.08.png>)
- Dane wizualizowane są za pomocą aplikacji webowej. Umozliwia ona wybór karty, z której wyświetlane są dane tranzakcji (liczba w nawiasie jest informacją dla uzytkownika ile tranzakcji zostało juz zarejestrowane dla danej karty). Ponizej wyświetlane są anomalie wykryte dla danej karty
    ![](<img/Screenshot 2024-06-13 at 14.58.43.png>)
- Ponadto dynamicznie odświeane są dwa wykresy z wartościami tranzakcji oraz czasem pomiędzy nimi,
    ![](<img/Screenshot 2024-06-13 at 14.58.22.png>)
    ![](<img/Screenshot 2024-06-13 at 15.03.30.png>)
- Oraz mapa z zaznaczonymi lokalizacjami tranzakcji
    ![](<img/Screenshot 2024-06-13 at 15.01.42.png>)
