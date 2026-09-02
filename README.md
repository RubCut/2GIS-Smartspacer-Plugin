<div align="center">

<img src="docs/icon.svg" width="112" alt="Иконка 2ГИС ETA для Smartspacer">

# 2ГИС ETA для Smartspacer

**Время в пути до выбранного места — прямо в Smartspacer**

[![Build APK](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml/badge.svg)](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)

</div>

Плагин добавляет в [Smartspacer](https://github.com/KieronQuinn/Smartspacer) три
Complication с расчётом ETA через API 2ГИС:

- 🚗 на автомобиле;
- 🚶 пешком;
- 🚌 на общественном транспорте.

Все три режима используют общий пункт назначения. Его можно найти по адресу через
Geocoder API или задать вручную широтой и долготой.

## Возможности

- современный экран настроек в стиле Material 3;
- динамические цвета на Android 12+;
- светлая и тёмная темы;
- интерфейс на русском и английском языках;
- выбор назначения по адресу или координатам;
- подсказка «Настроить» прямо в Complication до первого запуска;
- Complication остаётся видимой при временной ошибке и показывает `Нет ETA`;
- быстрый переход к карте и Platform Manager 2ГИС;
- немедленный расчёт ETA после сохранения;
- автоматическое обновление каждые 15 минут;
- параллельный расчёт автомобильного, пешего и транспортного маршрутов;
- отсутствие постоянного отслеживания геолокации.

## Требования

- Android 10 или новее;
- установленный Smartspacer;
- API-ключ 2ГИС с доступом к:
  - [Routing API](https://docs.2gis.com/api/navigation/routing/overview);
  - Public Transport API;
  - [Geocoder API](https://docs.2gis.com/api/search/geocoder/overview).

> API 2ГИС может быть платным и иметь ограничения по количеству запросов.
> Проверьте актуальные тарифы и квоты в Platform Manager перед использованием.

## Получение API-ключа

1. Откройте [Platform Manager 2ГИС](https://platform.2gis.ru/).
2. Создайте проект и ключ.
3. Подключите Routing API, Public Transport API и Geocoder API.
4. Скопируйте ключ в настройки плагина.

Ссылки на Platform Manager и документацию также доступны прямо на экране настроек.

## Установка

### Готовая debug-сборка

1. Откройте последнюю успешную сборку в разделе
   [Actions](https://github.com/RubCut/2GIS-Smartspacer-Plugin/actions/workflows/build.yml).
2. Скачайте артефакт `gis2smartspacer-debug-apk`.
3. Распакуйте архив и установите APK на телефон.

### Сборка из исходников

Требуются JDK 17, Android SDK 35 и Gradle 8.9.

```bash
git clone https://github.com/RubCut/2GIS-Smartspacer-Plugin.git
cd 2GIS-Smartspacer-Plugin
gradle assembleDebug
```

APK появится в каталоге:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Настройка

1. В Smartspacer добавьте одну или несколько Complication этого плагина:
   **Driving**, **Walking** или **Transit**.
2. Откройте **More settings** у добавленной Complication.
3. Вставьте API-ключ 2ГИС.
4. Выберите способ задания назначения:
   - **по адресу** — координаты определит Geocoder API;
   - **координаты** — найдите место на карте 2ГИС и введите широту и долготу.
5. Разрешите доступ к геолокации.
6. Нажмите **«Сохранить и обновить ETA»**.

Для обновления в фоне Android должен предоставить приложению доступ к геолокации
**«Разрешить в любом режиме» / “Allow all the time”**.

У плагина намеренно нет отдельной иконки в лаунчере: настройки открываются из
Smartspacer через **More settings**.

## Как работает обновление

```text
Smartspacer
    │ периодический запрос
    ▼
EtaComplicationUpdateReceiver
    │ последняя известная геопозиция
    ▼
2GIS Routing API
    │ ETA по запрошенным режимам
    ▼
SharedPreferences → Complication
```

`getSmartspaceActions()` не выполняет сетевые запросы. Провайдеры Complication
только читают уже сохранённый результат, поэтому Smartspacer получает ответ быстро.
Сетевые запросы выполняются фоновым receiver и параллелятся для сокращения времени
обновления.

## Геолокация и приватность

- плагин не запускает постоянное отслеживание перемещений;
- при сохранении запрашивается одна свежая точка с таймаутом;
- фоновые обновления используют последнюю известную Android геопозицию;
- API-ключ, адрес, координаты и кэш ETA хранятся локально в `SharedPreferences`;
- геопозиция и пункт назначения отправляются только в API 2ГИС для построения маршрута.

## Структура проекта

```text
app/src/main/java/com/rubcut/gis2smartspacer/
├── SettingsActivity.kt                 # экран настроек
├── SettingsRepository.kt               # локальные настройки и кэш
├── LocationHelper.kt                   # получение геопозиции
├── TwoGisClient.kt                     # Geocoder и Routing API
├── EtaUpdater.kt                       # параллельное обновление ETA
├── EtaComplicationUpdateReceiver.kt    # запросы обновления Smartspacer
└── complications/
    ├── BaseEtaComplication.kt
    ├── CarEtaComplication.kt
    ├── WalkEtaComplication.kt
    └── TransitEtaComplication.kt
```

## Ограничения

- точность ETA зависит от актуальности геопозиции и данных 2ГИС;
- до настройки Complication показывает `Настроить`, а при недоступном маршруте —
  `Нет ETA`; нажатие открывает экран настроек;
- текст Basic Complication ограничен 12 символами, поэтому используется короткий
  формат: `23 мин`, `1 ч 5 м`, `23 min`, `1 h 5 m`;
- три установленные Complication могут выполнять до трёх API-запросов каждые
  15 минут — учитывайте это при выборе тарифа.

## Отказ от ответственности

Проект не является официальным продуктом 2ГИС или Smartspacer. Названия и товарные
знаки принадлежат их правообладателям. Используя API 2ГИС, соблюдайте действующие
условия сервиса и ограничения вашего тарифа.
