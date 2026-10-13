package org.l4ncet;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String URL =
            "https://pildyk.lt/ru/refill";

    private static final Path PHONES_FILE =
            Path.of("src/main/resources/phones.txt");

    private static final Path CODES_FILE =
            Path.of("src/main/resources/codes.txt");

    private static final Path SCREENSHOTS_DIR =
            Path.of("src/main/resources/screenshots");

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        try {
            Files.createDirectories(SCREENSHOTS_DIR);

            WebDriver driver = new ChromeDriver();
            driver.manage().window().maximize();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            System.out.println("============================");
            System.out.println("       PILDYK");
            System.out.println("============================");

            // Первый запуск
            driver.get(URL);

            System.out.println();
            System.out.println("Прими cookies вручную.");
            System.out.println("Продолжаю через 10 секунд...");

            for (int i = 10; i >= 1; i--) {
                System.out.println(i + "...");
                Thread.sleep(1000);
            }

            int operation = 1;

            while (true) {

                List<String> phones = readLines(PHONES_FILE);
                List<String> codes = readLines(CODES_FILE);

                if (phones.isEmpty()) {
                    System.out.println();
                    System.out.println("Телефоны закончились.");
                    break;
                }

                if (codes.isEmpty()) {
                    System.out.println();
                    System.out.println("Коды закончились.");
                    break;
                }

                String originalPhone = phones.get(0);
                String phone = originalPhone.substring(4);
                String code = codes.get(0);

                System.out.println();
                System.out.println("============================");
                System.out.println("Операция #" + operation);
                System.out.println("Телефон: " + originalPhone);
                System.out.println("Код:     " + code);
                System.out.println("============================");

                // На второй и последующих итерациях
                // открываем страницу заново
                if (operation > 1) {
                    System.out.println("[1] Открываю страницу...");
                    driver.get(URL);
                }

                // Выбираем "КВИТАНЦИЯ ПОПОЛНЕНИЯ"
                System.out.println("[2] Выбираю КВИТАНЦИЯ ПОПОЛНЕНИЯ...");

                WebElement tab = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.id("refill-tab-1")
                        )
                );

                tab.click();

                // Проверяем, что вкладка реально выбрана
                wait.until(d ->
                        "true".equals(
                                d.findElement(
                                        By.id("refill-tab-1")
                                ).getAttribute("aria-selected")
                        )
                );

                System.out.println(
                        "[2] Вкладка выбрана."
                );

                // Ждём форму
                WebElement form = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.id("voucher-refill-form")
                        )
                );

                List<WebElement> inputs =
                        form.findElements(By.cssSelector("input"));

                if (inputs.size() < 2) {
                    throw new RuntimeException("Не найдены поля телефона и кода.");
                }

                WebElement phoneInput = inputs.get(0);
                WebElement codeInput = inputs.get(1);

                // Ввод телефона
                System.out.println("[3] Ввожу телефон: " + phone);

                phoneInput.clear();
                phoneInput.sendKeys(phone);

                Thread.sleep(300);

                // Ввод кода
                System.out.println("[4] Ввожу код: " + code);

                codeInput.clear();
                codeInput.sendKeys(code);

                Thread.sleep(300);

                // Находим кнопку ПРОДОЛЖИТЬ
                WebElement continueButton =
                        form.findElement(
                                By.cssSelector(".v-refill--form-button button")
                        );

                // Устанавливаем JS listener
                JavascriptExecutor js =
                        (JavascriptExecutor) driver;

                js.executeScript("""
                        window.manualContinueClicked = false;

                        arguments[0].addEventListener('click', function() {
                            window.manualContinueClicked = true;
                        });
                        """, continueButton);

                System.out.println();
                System.out.println("Данные введены.");

                System.out.println("Нажми ПРОДОЛЖИТЬ вручную в браузере.");

                System.out.println("Ожидаю нажатия...");

                // Ждём ручного нажатия
                while (true) {

                    try {
                        Object result =
                                js.executeScript("return window.manualContinueClicked;");

                        if (Boolean.TRUE.equals(result)) {
                            break;
                        }

                    } catch (Exception ignored) {
                    }

                    Thread.sleep(100);
                }

                System.out.println();
                System.out.println("Кнопка ПРОДОЛЖИТЬ нажата.");

                // Отсчёт до screenshot
                System.out.println();

                for (int i = 5; i >= 1; i--) {

                    System.out.println(
                            "Скриншот через " + i + "..."
                    );

                    Thread.sleep(1000);
                }

                // Screenshot
                Path screenshot =
                        takeScreenshot(
                                driver,
                                originalPhone
                        );

                System.out.println();
                System.out.println(
                        "Скриншот сделан:"
                );

                System.out.println(
                        screenshot
                );

                System.out.println();
                System.out.println(
                        "Проверь результат в браузере."
                );

                System.out.println(
                        "Если всё нормально — нажми ENTER."
                );

                System.out.println(
                        "До ENTER файлы НЕ изменяются."
                );

                // Ждём ручного ENTER
                scanner.nextLine();

                // Только теперь удаляем текущую пару
                removeFirstLine(PHONES_FILE);
                removeFirstLine(CODES_FILE);

                System.out.println(
                        "Текущий телефон и код удалены из файлов."
                );

                System.out.println(
                        "Перехожу к следующей паре..."
                );

                operation++;

                Thread.sleep(1000);
            }

            System.out.println();
            System.out.println("============================");
            System.out.println("Все данные обработаны.");
            System.out.println("============================");

        } catch (Exception e) {

            System.out.println();
            System.out.println("============================");
            System.out.println("ОШИБКА ПРОГРАММЫ");
            System.out.println(e.getMessage());
            System.out.println("============================");

            e.printStackTrace();
        }
    }


    private static Path takeScreenshot(
            WebDriver driver,
            String phone
    ) throws IOException {

        String timestamp =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd_HH-mm-ss"
                        )
                );

        Path destination =
                SCREENSHOTS_DIR.resolve(
                        phone + "_" + timestamp + ".png"
                );

        byte[] screenshot =
                ((TakesScreenshot) driver)
                        .getScreenshotAs(
                                OutputType.BYTES
                        );

        Files.write(
                destination,
                screenshot
        );

        return destination;
    }


    private static List<String> readLines(
            Path path
    ) throws IOException {

        return new ArrayList<>(
                Files.readAllLines(path)
        );
    }


    private static void removeFirstLine(
            Path path
    ) throws IOException {

        List<String> lines =
                new ArrayList<>(
                        Files.readAllLines(path)
                );

        if (lines.isEmpty()) {
            return;
        }

        lines.remove(0);

        Files.write(
                path,
                lines,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}