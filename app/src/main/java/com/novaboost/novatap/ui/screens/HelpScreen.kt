package com.novaboost.novatap.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel

@Composable
fun HelpScreen(
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()
    val isRu = viewModel.selectedLanguage == "ru"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpCenter,
                    contentDescription = "Help Center",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = if (isRu) "Справочный центр NovaTap" else "NovaTap Help Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRu) "Тонкости автокликера и защитного алгоритма" else "Get started with next-gen stochastic gestures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Text(
            text = if (isRu) "ДОСТУПНЫЕ ИНСТРУКЦИИ" else "AVAILABLE ARTICLES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            letterSpacing = 1.1.sp
        )

        // Guide 1: Single Clicker
        HelpSectionCard(
            title = if (isRu) "Одиночное Нажатие — Клик в точке" else "Single Click Target Point",
            desc = if (isRu) {
                "Позволяет эмулировать клик в фиксированных координатах. \n\n" +
                "Инструкции по применению:\n" +
                "1. Введите X и Y координаты цели.\n" +
                "2. Укажите интервал нажатия (минимально 40мс).\n" +
                "3. Включите Движок Human Touch для плавных перемещений и дрожания."
            } else {
                "Plopes target inputs at precise coordinate positions with constant recurrence.\n\n" +
                "Procedure:\n" +
                "1. Type desired targets absolute coordinates inside fields (X, Y).\n" +
                "2. Set timings parameters in ms (minimum boundary of 40ms).\n" +
                "3. Use Human Touch to introduce slight positional noise."
            },
            icon = Icons.Default.AdsClick,
            isRu = isRu
        )

        // Guide 2: Multi Clicker
        HelpSectionCard(
            title = if (isRu) "Мульти-нажатие — Последовательности" else "Multi-Point Sequencing",
            desc = if (isRu) {
                "Полезно для выполнения повторяющихся циклов в нескольких местах экрана.\n\n" +
                "Шаги для настройки:\n" +
                "1. Нажмите на чёрный холст симулятора, чтобы добавить точки (до 20 штук).\n" +
                "2. Выберите режим: «Последовательный» (node 1 -> node 2), «Случайный» или «Цикличный».\n" +
                "3. Нажмите Запуск, чтобы задействовать цепочки."
            } else {
                "Executes tap-sequences inside several coordinate nodes. Helpful for grinding games.\n\n" +
                "Steps:\n" +
                "1. Point coordinates layout by tapping on simulated preview area (max 20).\n" +
                "2. Choose playorder models: Sequential, Random order, or Loop sequence.\n" +
                "3. Press Play sequence to trigger operations."
            },
            icon = Icons.Default.Stream,
            isRu = isRu
        )

        // Guide 3: Area Zone Clicks (Flagship)
        HelpSectionCard(
            title = if (isRu) "Флагманские Зоны (Safe Farming) 🔥" else "Stochastic Area Zones (Flagship) 🔥",
            desc = if (isRu) {
                "Самый передовой режим, практически неопределяемый защитными блокировками!\n\n" +
                "Как это устроено:\n" +
                "1. Вы создаёте зелёные зоны (разрешённая игра) и красные зоны (блокированные элементы).\n" +
                "2. Наш стохастический движок генерирует случайные позиции кликов исключительно внутри зелёной зоны.\n" +
                "3. Если координаты клика совпадают с красными зонами, клик автоматически переносится ради безопасности!"
            } else {
                "Our most advanced kliker technique configured specifically to bypass standard clicks guards.\n\n" +
                "Concept:\n" +
                "1. Paint Green Allowed circles & Red Obstacles zones directly inside layout previewer.\n" +
                "2. Stochastic engine spawns randomized clicked vectors inside valid zone matrices.\n" +
                "3. Interception loops bypass red spots automatically. Maximum protection."
            },
            icon = Icons.Default.FilterCenterFocus,
            isRu = isRu
        )

        // Guide 4: Swipe movements
        HelpSectionCard(
            title = if (isRu) "Слайды и Свайпы — Линейный дрейф" else "Gestures Swipes linear glide",
            desc = if (isRu) {
                "Имитирует непрерывные свайп движения пальцами на экране.\n\n" +
                "Процедура:\n" +
                "1. Координата старта определяет место касания.\n" +
                "2. Скорость свайпа в мс регулирует длительность перемещения пальцев.\n" +
                "3. Опция «Изогнутый путь» эмулирует реальное нелинейное ускорение движений."
            } else {
                "Simulates linear drag and continuous glide movements of fingers.\n\n" +
                "Usage:\n" +
                "1. Adjust Start coordinate point (touchdown anchor spot).\n" +
                "2. Duration settings in ms controls finger motion glide velocity.\n" +
                "3. Activation of Curved paths forces stochastic path curves mimicking true fingers."
            },
            icon = Icons.Default.Swipe,
            isRu = isRu
        )

        // Guide 5: Scenario chains
        HelpSectionCard(
            title = if (isRu) "Конструктор сценариев и алгоритмы" else "Complex Scenarios Workflow chains",
            desc = if (isRu) {
                "Идеально для сложных поведенческих цепочек!\n\n" +
                "Как собрать цепочку:\n" +
                "1. Добавляйте блоки команд в плейлист: Клик, Зона, Свайп или Ожидание ( Wait ).\n" +
                "2. Задайте имя вашему сценарию.\n" +
                "3. Запустите цикл, чтобы запустить выполнение сверху-вниз!"
            } else {
                "Best for setting deep structural automation macros.\n\n" +
                "Workflow:\n" +
                "1. Accumulate command step lists: Tap Point, Wait delay, Swipe glide or Area Touch.\n" +
                "2. Type name for save index.\n" +
                "3. Play scenarios chain executing vertical hierarchy."
            },
            icon = Icons.Default.PlayForWork,
            isRu = isRu
        )

        // Guide 6: Permissions
        HelpSectionCard(
            title = if (isRu) "Важные разрешения и Безопасность" else "Critical Guidelines & Permissions",
            desc = if (isRu) {
                "Для реального автоклика NovaTap необходимы разрешения системы:\n" +
                "1. СЛУЖБА СПЕЦИАЛЬНЫХ ВОЗМОЖНОСТЕЙ: Нужна исключительно для физического нажатия и эмуляции жестов по экрану. Личные данные не собираются.\n" +
                "2. ОВЕРЛЕЙ Поверх приложений: Нужен для отрисовки летающей панели управления во внешних играх или браузерах.\n" +
                "3. ИСКЛЮЧЕНИЕ БАТАРЕИ: Прерывает засыпание и заморозку кликов в фоне."
            } else {
                "Automating clicks actions on Android demands system rules access:\n" +
                "1. ACCESSIBILITY CORE SERVICE: Exclusively required to dispatch target tactile touch clicks. Strictly zero personal data tracking.\n" +
                "2. DRAWS OVER APPLICATIONS OVERLAY: Places handy floating control controllers atop separate apps.\n" +
                "3. BATTERY EXEMPTIONS: Blocks Android system from freezing automation streams."
            },
            icon = Icons.Default.Settings,
            isRu = isRu
        )

        // Guide 7: Premium, Limits & Ad Rules
        HelpSectionCard(
            title = if (isRu) "Лимиты, Реклама и Premium подписка" else "Daily Limits, Ads & Premium Pass",
            desc = if (isRu) {
                "В приложении действуют следующие правила для оптимальной работы:\n\n" +
                "1. PREMIUM ПОЛЬЗОВАТЕЛИ: Абсолютно никаких ограничений, рекламы и лимитов! Вы можете совершать хоть миллиард кликов без сбросов и задержек.\n" +
                "2. БЕСПЛАТНАЯ ВЕРСИЯ: Включает лимит 50 000 кликов (действий) каждые 24 часа.\n" +
                "3. ДОБАВЛЕНИЕ КЛИКОВ ЗА РЕКЛАМУ: Неоплатившие пользователи могут получить еще +10 000 кликов за просмотр 30-секундного спонсорского видео. Допускается максимум 3 рекламы (+30 000 кликов) за 15 минут, после чего таймер сбросится и можно посмотреть снова.\n" +
                "4. МЕЖСТРАНИЧНАЯ РЕКЛАМА: Короткая 5-секундная пропускаемая реклама показывается бесплатным пользователям при возвращении в приложение (не чаще раза в 45 секунд)."
            } else {
                "The application operates under the following resource & feature guidelines:\n\n" +
                "1. PREMIUM USERS: Absolutely no restrictions, no ads, and no limits! You can make billions of clicks with infinite freedom and no resets needed.\n" +
                "2. FREE TIER: Grants a starting limit of 50,000 auto-clicks (actions) every 24 hours.\n" +
                "3. ADD CLICKS VIA ADS: Non-premium users can add +10,000 clicks by watching a 30-second sponsored video. You can watch up to 3 ads (+30,000 clicks) per 15 minutes, after which you can watch another 3 ads.\n" +
                "4. INTERSTITIAL ADS: Quick 5-second skippable ad shows to free users when returning to the app (no more than once per 45s)."
            },
            icon = Icons.Default.Star,
            isRu = isRu
        )

        // Guide 8: Themes & App Customization
        HelpSectionCard(
            title = if (isRu) "Темы оформления и Интерфейс" else "Themes & Appearance Setup",
            desc = if (isRu) {
                "Настройте внешний вид приложения под себя:\n\n" +
                "1. ВЫБОР ТЕМЫ: В меню Настроек доступны три режима: Тёмная, Светлая и Системная тема.\n" +
                "2. СИНХРОНИЗАЦИЯ: Системная тема автоматически адаптирует NovaTap под настройки вашего Android-устройства.\n" +
                "3. ПЛАВАЮЩАЯ ПАНЕЛЬ: Все элементы управления на плавающей панели адаптируют свои цвета под выбранную тему автоматически."
            } else {
                "Tailor the visuals and style of your clicker workspace:\n\n" +
                "1. THEME SELECTION: Toggle between Dark, Light, or System themes inside the Settings tab.\n" +
                "2. SYSTEM SYNC: Selecting System Theme aligns NovaTap aesthetics with your Android device's global settings.\n" +
                "3. FLOATING PANEL SUPPORT: All workspace tools and controllers dynamically adjust colors according to your active theme."
            },
            icon = Icons.Default.Palette,
            isRu = isRu
        )
    }
}

@Composable
fun HelpSectionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isRu: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), lineHeight = 20.sp)
                }
            }
        }
    }
}
