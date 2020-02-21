package com.xpp.unzip;

import com.github.junrar.UnrarCallback;
import com.github.junrar.Volume;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {
    private VBox centerBox;
    private static final double WIDTH = 600d;
    private static final double HIGTH = 500d;
    private Label label, statusLabel;
    private StringBuilder sb = new StringBuilder();
    private ExecutorService executorService;
    private BorderPane borderPane;

    public static void main(String[] args) {

        launch(args);
    }


    private List<File> fileList;


    @Override
    public void start(Stage primaryStage) {
        borderPane = new BorderPane();
        Scene scene = new Scene(borderPane, WIDTH, HIGTH);
        primaryStage.setWidth(WIDTH);
        primaryStage.setHeight(HIGTH);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        initView();
    }


    private void initView() {

        centerBox = new VBox();
        centerBox.setPadding(new Insets(10, 10, 10, 10));
        centerBox.setSpacing(10);
        centerBox.setAlignment(Pos.TOP_LEFT);
        centerBox.setBackground(Background.EMPTY);

        label = new Label("请将zip、rar、7z文件拖动到此区域，不支持加密压缩包解压");
        label.setFont(Font.font(16));
        centerBox.getChildren().addAll(label);

        centerBox.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getGestureSource() != centerBox) {
                    event.acceptTransferModes(TransferMode.ANY);
                }
            }
        });

        centerBox.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (isExecute) {
                    return;
                }
                Dragboard dragboard = event.getDragboard();
                List<File> files = dragboard.getFiles();
                findCompress(files);
            }
        });
        borderPane.setCenter(centerBox);

        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(5, 5, 5, 5));
        statusLabel = new Label();
        statusLabel.setFont(Font.font(12));
        statusLabel.setTextFill(Color.valueOf("D81B60"));
        bottomBox.getChildren().addAll(statusLabel);
        borderPane.setBottom(bottomBox);

    }

    private void findCompress(List<File> files) {
        if (files != null && files.size() > 0) {
            if (fileList == null) {
                fileList = new ArrayList<>();
            } else {
                fileList.clear();
            }
            index = 0;
            fileList.addAll(files);
            executorService = Executors.newSingleThreadExecutor();
            parseList();
        }
    }

    private void showProgress() {
        Platform.runLater(new Runnable() {
            public void run() {
                label.setText(sb.toString());
            }
        });
    }


    private void addProgressStr(String str) {
        sb.append(str + "\n");
        showProgress();
    }


    private int index = 0;

    private void parseList() {

        if (index < fileList.size()) {
            execute(fileList.get(index));
        } else {
            addProgressStr("=======执行完成！=========");
            isExecute = false;
            index = 0;
            executorService.shutdown();
        }

    }


    private boolean isExecute;

    private void execute(File apkFile) {
        sb.delete(0, sb.length());

        if (!apkFile.exists()) {
            addProgressStr("文件不存在！");
            stopExe();
            return;
        }
        if (apkFile.isFile() && (apkFile.getName().toLowerCase().endsWith(".zip") || apkFile.getName().toLowerCase().endsWith(".rar") || apkFile.getName().toLowerCase().endsWith(".7z"))) {
            isExecute = true;
            executorService.execute(new Executer(apkFile));
        } else {
            addProgressStr(apkFile.getName() + "不是压缩文件！");
            stopExe();
        }


    }


    private class Executer implements Runnable {

        private File apkFile;

        public Executer(File apkFile) {
            this.apkFile = apkFile;
        }

        @Override
        public void run() {
            addProgressStr("解压缩：" + apkFile.getName());

            addProgressStr("1.开始解压缩");
            String s = apkFile.getName().toLowerCase();
            String unCompressPath = null;
            if (s.endsWith(".zip")) {
                unCompressPath = ZipUtils.unZip(apkFile.getPath());
            } else if (s.endsWith(".rar")) {
                try {
                    unCompressPath = RarUtils.unrar(apkFile.getPath(), new UnrarCallback() {
                        @Override
                        public boolean isNextVolumeReady(Volume volume) {
                            return true;
                        }

                        int currentProgress = -1;

                        @Override
                        public void volumeProgressChanged(long l, long l1) {
                            int progress = (int) ((double) l / l1 * 100);
                            if (currentProgress != progress) {
                                currentProgress = progress;
                                System.out.println("Unrar " + apkFile.getName() + " rate: " + progress + "%");
                            }
                        }
                    });
                } catch (Exception e) {
                    addProgressStr(apkFile.getName() + e.getMessage());
                }

            } else if (s.endsWith(".7z")) {
                try {
                    unCompressPath = SevenZUtils.unCompress(apkFile.getPath());
                } catch (Exception e) {
                    addProgressStr(apkFile.getName() + e.getMessage());
                }
            }

            if (unCompressPath == null) {
                addProgressStr("  解压缩失败！");
                stopExe();
                return;
            }
            addProgressStr("  解压缩完成!");

            File dirFile = new File(unCompressPath);
            if (!dirFile.exists()) {
                addProgressStr("  解压后文件夹不存在！");
                stopExe();
                return;
            }

            File[] files = dirFile.listFiles();
            if (files == null || files.length == 0) {
                addProgressStr("  解压后文件夹无文件！");
                stopExe();
                return;
            }

            stopExe();
        }
    }

    private void stopExe() {
        index++;
        parseList();
    }

}
