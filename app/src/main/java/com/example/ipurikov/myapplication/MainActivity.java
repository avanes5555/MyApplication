package com.example.ipurikov.myapplication;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.atol.drivers10.fptr.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_SHOW_SETTINGS = 1;
    private static final int REQUEST_CALL_SERVICE = 2;
    private IFptr fptr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnShowSettings).setOnClickListener(this);
        findViewById(R.id.btnPrintReceipt).setOnClickListener(this);
        findViewById(R.id.btnintent).setOnClickListener(this);
        findViewById(R.id.btnX).setOnClickListener(this);
        findViewById(R.id.btnOpen).setOnClickListener(this);



        // Создание объекта компонента
        fptr = new Fptr(getApplication());

        // Начальная инициализация настройками (тут они могут вычитываться из хранилища приложения, например)
        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_PORT, String.valueOf(IFptr.LIBFPTR_PORT_BLUETOOTH));
        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_MACADDRESS, "34:87:3D:5C:BC:12");
        fptr.setSingleSetting(IFptr.LIBFPTR_SETTING_OFD_CHANNEL, String.valueOf(IFptr.LIBFPTR_OFD_CHANNEL_PROTO));
        
        fptr.applySingleSettings();
    }

    @Override
    public void onClick(View view) {

         if (view.getId() == R.id.btnintent) {
            Intent i = new Intent();
            i.setAction("ru.atol.drivers10.service.PROCESS_TASK");
            i.putExtra("PARAM_REQUEST", "{\n" +
                    "    \"type\": \"reportOfdExchangeStatus\",\n" +
                    "    \"operator\": {\n" +
                    "       \"name\": \"Иванов\",\n" +
                    "       \"vatin\": \"123654789507\"\n" +
                    "    }\n" +
                    "}");
            startActivityForResult(i, REQUEST_CALL_SERVICE);

        }
        else if (view.getId() == R.id.btnShowSettings) {
            Intent intent = new Intent(getApplication(), SettingsActivity.class);
            // Передаем текущие настройки в SettingsActivity.
            // Если не передать - будет показана SettingsActivity с настройками по умолчанию
            intent.putExtra(SettingsActivity.DEVICE_SETTINGS, fptr.getSettings());
            startActivityForResult(intent, REQUEST_SHOW_SETTINGS);
        }

         else if (view.getId() == R.id.btnOpen) {
             new Thread(new Runnable() {
                 @Override
                 public void run() {

                     fptr.close();
                     //fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_CLOSE_SHIFT);
                     //fptr.report();
                 }

             }).start();
         }


         else if (view.getId() == R.id.btnX) {
             new Thread(new Runnable() {
                 @Override
                 public void run() {

                     fptr.open();
                     //fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_X);
                     fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_OFD_EXCHANGE_STATUS);
                     fptr.report();
                     fptr = new Fptr(getApplication());
                 }

             }).start();
         }
         // for commit 1
         // for commit 2

        else if (view.getId() == R.id.btnPrintReceipt) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Соединение с ККТ
                    fptr.open();

                    // Регистрация кассира
                    fptr.setParam(1021, "Иванов И.И.");
                    fptr.setParam(1203, "500100732259");
                    fptr.operatorLogin();

                    // Открытие электронного чека (с передачей телефона получателя)
                    fptr.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_TYPE, IFptr.LIBFPTR_RT_SELL);
                    fptr.setParam(1008, "+79161234567");
                    fptr.openReceipt();

                    // Регистрация позиции
                    fptr.setParam(IFptr.LIBFPTR_PARAM_COMMODITY_NAME, "Чипсы LAYS");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_PRICE, 73.99);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_QUANTITY, 5);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_TAX_TYPE, IFptr.LIBFPTR_TAX_VAT18);
                    fptr.setParam(1212, 1);
                    fptr.setParam(1214, 7);
                    fptr.registration();

                    // Регистрация итога (отрасываем копейки)
                    fptr.setParam(IFptr.LIBFPTR_PARAM_SUM, 369.0);
                    fptr.receiptTotal();

                    // Оплата наличными
                    fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_TYPE, IFptr.LIBFPTR_PT_CASH);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_SUM, 1000);
                    fptr.payment();

                    // Закрытие чека
                    if (fptr.closeReceipt() < 0) {
                        // Обработка исключительных ситуаций, если произошла ошибка при закрытии чека
                        // (чек мог закрыться на самом деле, даже если, например, закончилась чековая лента)
                        fptr.checkDocumentClosed();
                        if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_CLOSED)) {
                            // Обработка ошибки закрытия чека
                            return;
                        }
                    }

                    // Запрос информации о закрытом чеке
                    fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
                    fptr.fnQueryData();
                    final String fiscalSign = fptr.getParamString(IFptr.LIBFPTR_PARAM_FISCAL_SIGN);
                    final long documentNumber = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    String.format("ФПД: %s\nФД: %d",
                                            fiscalSign,
                                            documentNumber),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    // Формирование слипа ЕГАИС
                    fptr.beginNonfiscalDocument();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, "ИНН: 111111111111 КПП: 222222222");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.printText();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, "КАССА: 1               СМЕНА: 11");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.printText();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, "ЧЕК: 314  ДАТА: 20.11.2017 15:39");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.printText();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_BARCODE, "https://check.egais.ru?id=cf1b1096-3cbc-11e7-b3c1-9b018b2ba3f7");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_BARCODE_TYPE, IFptr.LIBFPTR_BT_QR);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_SCALE, 5);
                    fptr.printBarcode();

                    fptr.printText();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT, "https://check.egais.ru?id=cf1b1096-3cbc-11e7-b3c1-9b018b2ba3f7");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT_WRAP, IFptr.LIBFPTR_TW_CHARS);
                    fptr.printText();

                    fptr.printText();

                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT,
                            "10 58 1c 85 bb 80 99 84 40 b1 4f 35 8a 35 3f 7c " +
                                    "78 b0 0a ff cd 37 c1 8e ca 04 1c 7e e7 5d b4 85 " +
                                    "ff d2 d6 b2 8d 7f df 48 d2 5d 81 10 de 6a 05 c9 " +
                                    "81 74");
                    fptr.setParam(IFptr.LIBFPTR_PARAM_ALIGNMENT, IFptr.LIBFPTR_ALIGNMENT_CENTER);
                    fptr.setParam(IFptr.LIBFPTR_PARAM_TEXT_WRAP, IFptr.LIBFPTR_TW_WORDS);
                    fptr.printText();

                    fptr.endNonfiscalDocument();

                    // Отчет о закрытии смены
                    //fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_CLOSE_SHIFT);
                    //fptr.report();

                    // Получение информации о неотправленных документах
                    fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
                    fptr.fnQueryData();

                    final long unsentCount = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENTS_COUNT);
                    final long unsentFirstNumber = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
                    DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                    final String unsentDateTime = df.format(fptr.getParamDateTime(IFptr.LIBFPTR_PARAM_DATE_TIME));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    String.format("Статус обмена с ОФД: %d неотправленно, первый: №%d (%s)",
                                            unsentCount,
                                            unsentFirstNumber,
                                            unsentDateTime),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }).start();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == Activity.RESULT_OK) {
            // Записываем настройки в объект
            fptr.setSettings(data.getStringExtra(SettingsActivity.DEVICE_SETTINGS));
        } else if (requestCode == REQUEST_CALL_SERVICE)  {
           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            data.getStringExtra("PARAM_RESULT"),
                            Toast.LENGTH_LONG).show();
                }
            });*/

        }
    }



    @Override
    public void onBackPressed() {
        // Гарантировано чистим объект.
        // Вызов этого метода так же разрывает соединение, если оно установлено.
        //fptr.destroy();
        super.onBackPressed();
    }
}
