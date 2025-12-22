# Real Estate Manager (Android)

A lightweight Android app for managing real estate objects (apartments/commercial units), tracking income/expenses, and generating clean PDF reports for a selected period.

---

## 🇬🇧 English

### What is this app?
**Real Estate Manager** helps you keep finances for multiple properties in one place:
- Create and manage **properties**
- Add **transactions** (income/expense) with date and comment
- View **stats** by month/year and generate **period reports**
- Export a **beautiful PDF report** for a selected range and share it via messengers/email

### Key features
- Property filter (view stats per property or across all)
- Tabs: **Month / Year / Period**
- Period reporting:
  - Choose exact **From / To** dates
  - Totals: **Income / Expense / Net**
  - Average net per month (for the selected range)
- PDF export:
  - Clean header, optional property avatar, multi-page layout
  - Monthly summary + detailed transactions

### Tech stack
- **Kotlin**
- **Jetpack Compose** UI
- **MVVM** (ViewModel)
- **Room** database (offline-first)
- PDF generation via **PdfDocument/Canvas**
- Android Share via **FileProvider**

### Possible scaling / roadmap ideas
- Attachments for transactions (receipts, invoices, photos, PDFs)
- Categories, tags, recurring transactions, and reminders
- Advanced analytics: cashflow charts, forecasts, occupancy/vacancy, ROI
- Cloud sync (multi-device), backups, and user accounts
- Multi-currency + exchange rates, export to Excel/CSV
- Multiple owners/teams, roles and permissions
- Integrations: bank import, Google Drive, email parsing for bills

---

## 🇵🇹 Português (Portugal)

### O que é esta app?
**Real Estate Manager** é uma aplicação Android para gerir imóveis e controlar finanças:
- Criar e gerir **imóveis**
- Registar **transações** (receitas/despesas) com data e comentário
- Ver **estatísticas** por mês/ano e gerar **relatórios por período**
- Exportar um **PDF** bem apresentado para um intervalo de datas e partilhar por mensagens/email

### Funcionalidades principais
- Filtro por imóvel (ver dados por imóvel ou todos)
- Separadores: **Mês / Ano / Período**
- Relatório por período:
  - Seleção exata de datas **De / Até**
  - Totais: **Receitas / Despesas / Resultado**
  - Média mensal do resultado (no intervalo escolhido)
- Exportação PDF:
  - Cabeçalho limpo, avatar do imóvel opcional, várias páginas
  - Resumo mensal + detalhamento de transações

### Stack tecnológica
- **Kotlin**
- UI com **Jetpack Compose**
- **MVVM** (ViewModel)
- Base de dados **Room** (offline-first)
- Geração de PDF com **PdfDocument/Canvas**
- Partilha Android via **FileProvider**

### Ideias de escalabilidade / evolução
- Anexos em transações (recibos, faturas, fotos, PDFs)
- Categorias, etiquetas, transações recorrentes e lembretes
- Analytics avançado: gráficos de cashflow, previsões, ocupação, ROI
- Sincronização cloud (vários dispositivos), backups e contas de utilizador
- Multi-moeda + taxas de câmbio, exportação para Excel/CSV
- Vários proprietários/equipa, permissões e papéis
- Integrações: importação bancária, Google Drive, leitura de faturas por email

---

## 🇷🇺 Русский

### Что это за приложение?
**Real Estate Manager** — Android-приложение для учёта недвижимости и финансов:
- Создание и ведение **объектов недвижимости**
- Добавление **транзакций** (доход/расход) с датой и комментарием
- Просмотр **статистики** по месяцу/году и формирование **отчёта за период**
- Экспорт **красивого PDF-отчёта** за выбранный диапазон с возможностью поделиться

### Основные возможности
- Фильтр по объекту (статистика по одному объекту или по всем)
- Вкладки: **Месяц / Год / Период**
- Отчёт за период:
  - Выбор дат **С / По**
  - Итоги: **Доход / Расход / Итого (чистая)**
  - Средняя чистая выручка в месяц за период
- PDF-экспорт:
  - Аккуратная шапка, опциональная аватарка объекта, многостраничный отчёт
  - Сводка по месяцам + детальная таблица транзакций

### Технологии
- **Kotlin**
- UI на **Jetpack Compose**
- **MVVM** (ViewModel)
- База данных **Room** (offline-first)
- Генерация PDF через **PdfDocument/Canvas**
- Поделиться файлом через **FileProvider**

### Идеи для масштабирования
- Вложения к транзакциям (чеки, счета, фото, PDF)
- Категории/теги, регулярные платежи, напоминания
- Расширенная аналитика: графики, прогноз, окупаемость, загрузка объектов
- Облачная синхронизация, бэкапы, аккаунты пользователей
- Мультивалюта и курсы, экспорт в Excel/CSV
- Командная работа: роли/права доступа
- Интеграции: импорт из банка, Google Drive, разбор писем со счетами

---

## Build & run (quick)
1. Open the project in **Android Studio**
2. Sync Gradle
3. Run `app` on an emulator or device
4. If `./gradlew :app:installDebug` fails to spawn `adb`, run `./gradlew --stop` and retry, or use `./gradlew --no-daemon :app:installDebug`.

---

## Data safety (do not wipe current DB)
- Never uninstall the app or use "Clear storage" in Android settings.
- Avoid `adb uninstall` and `adb shell pm clear` for `com.example.my_project`.
- Use `./gradlew :app:installDebug` or `adb install -r` for updates (keep data).
- Before any risky changes, back up `/data/data/com.example.my_project/databases/real_estate.db` and restore only via `run-as`.

### Backup & restore script
Use `scripts/backup_restore.sh` to save/restore both DB and files:
- `./scripts/backup_restore.sh backup`
- `./scripts/backup_restore.sh backup /path/to/dir`
- `./scripts/backup_restore.sh restore /path/to/backup_dir`
