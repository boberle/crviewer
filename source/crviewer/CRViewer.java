/*
 *
 * CRViewer -- Computer co-reference chain statistics.
 * 
 * Copyright 2016-2017 Bruno Oberlé.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * This program comes with ABSOLUTELY NO WARRANTY.  See the Mozilla Public
 * License, v. 2.0 for more details.
 * 
 * Some questions about the license may have been answered at
 * https://www.mozilla.org/en-US/MPL/2.0/FAQ/.
 * 
 * If you have any question, contact me at boberle.com.
 * 
 * The source code can be found at boberle.com.
 *
 */

package crviewer;

import javafx.application.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Callback;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;


// TODO: t00s01: only one paragraph?????
// TODO: split, reject < MIN, then apply filter?

enum DisplayUnit {
   corpus, text, part, parttype, paragraph, chain
}

enum DisplayType {
   concordancer, stats, statsBar, frequencies, pie
}

enum StatsBar {
   getAverageLinkToLinkDistance, getAverageLinkLength, getAverageChainSize, getChainCount, getLinkCount, getChainDensity, getLinkDensity, getAnnotationDensity, getTokenCount
}

public class CRViewer extends Application {
   private class PropertyControlPair {
      TextField tfProp = new TextField("");
      ComboBox<String> cboProp = new ComboBox<>();
   }
   private static final int CHAIN_MIN_SIZE = 3;
   private static final int PROPERTY_CONTROL_NUMBER = 4;
   private static final int CONTEXT_WIDTH = 5;
   private static final int PNG_WIDTH = 1280;
   private static final int PNG_HEIGHT = 800;
   private Stage primaryStage;
   private Corpus corpus;
   private PartType[] partTypes;
   private SplitPane rootpane;
   private TextField tfMinSize;
   private TextField tfRefName;
   private ComboBox<String> cboRefName;
   private TextField tfTextId;
   private PropertyControlPair[] ctrlProperties;
   private ComboBox<String> cboSplitBy;
   private ComboBox<String> cboDisplayUnit;
   private ComboBox<String> cboDisplayType;
   private ComboBox<String> cboXProperty;
   private ComboBox<String> cboYStat;
   private TextField tfDisplayFilter;
   private TextField tfContextWidth;
   private ComboBox<String> cboStabCoeffProperty;
   private TextField tfStabCoeffValue;
   
   
   /* Returns a TVS by loop around a TableView. Adapted from
    * https://stackoverflow.com/questions/26815442/javafx-8-iterate-through-tableview-cells-and-get-graphics
    */
   private static String getTsvFromTableView(TableView tableView) {
      //ArrayList<String> values = new ArrayList<>();
      StringBuilder tsv = new StringBuilder();
      ObservableList<TableColumn> columns = tableView.getColumns();
      // headings
      boolean firstCol = true;
      for (TableColumn column : columns) {
         if (!firstCol) {
            tsv.append("\t");
         }
         firstCol = false;
         String heading = column.getText().trim().replace('\n', ' ');
         tsv.append(heading);
      }
      // body
      for (Object row : tableView.getItems()) {
         tsv.append("\n");
         firstCol = true;
         for (TableColumn column : columns) {
            if (!firstCol) {
               tsv.append("\t");
            }
            firstCol = false;
            String cell = column.getCellObservableValue(row).getValue().toString().trim();
            tsv.append(cell);
            //values.add();
         }
      }
      //return values;
      return tsv.toString();
   }
   
   private static void writeTsv(String tsv, String filename) {
      String fn = filename + ".tsv";
      try {
         FileWriter fw = new FileWriter(fn);
         fw.write(tsv);
         fw.close();
         System.out.println("Table written to `"+fn+"'.");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /* look at https://stackoverflow.com/questions/33928139/javafx-graphs-screenshot
    * and https://stackoverflow.com/questions/29721289/how-to-generate-chart-image-using-javafx-chart-api-for-export-without-displying
    */
   private static void writeChart(Chart chart, String filename) {
      String fn = filename + ".png";
      Scene scene = new Scene(chart, CRViewer.PNG_WIDTH, CRViewer.PNG_HEIGHT);
      //Scene scene = new Scene(chart, 1440, 900);
      WritableImage image = chart.snapshot(new SnapshotParameters(), null);
      File file = new File(fn);
      try {
         ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
         System.out.println("Image written to `"+fn+"'.");
      } catch (IOException e) {
         e.printStackTrace();
      }      
   }


   public CRViewer() {
   }
   
   private void showInfoBox(String text) {
      Pane root = new FlowPane();
      Scene scene = new Scene(root, 300, 200);
      Stage stage = new Stage();
      stage.setTitle("Infos");
      stage.setScene(scene);
      Label label = new Label(text);
      Button btn = new Button("OK!");
      btn.setOnAction(new EventHandler<ActionEvent>() {
         @Override
         public void handle(ActionEvent event) {
           stage.close();
         }
      });
      root.getChildren().addAll(label, btn);
      stage.initModality(Modality.WINDOW_MODAL);
      stage.initOwner(this.primaryStage);
      stage.show();
   }


   private boolean createCorpus(Stage stage) throws IOException,ParseException {
      this.corpus = new Corpus();
      ArrayList<String> filenames = new ArrayList<>();
      if (this.getParameters().getRaw().size()==0) {
         FileChooser fileChooser = new FileChooser();
         fileChooser.setTitle("Open Text File");
            List<File> files = fileChooser.showOpenMultipleDialog(stage);
            if (files == null) {
               return false;
            } else {
               for (File file : files) {
                  filenames.add(file.getAbsolutePath());
               }
            }
      } else {
         for (String arg : this.getParameters().getRaw()) {
            filenames.add(arg);
         }
      }
      for (String filename : filenames) {
         FileParser fp = new FileParser(filename);
         corpus.addText(fp.getText());
      }
      this.partTypes = new PartType[5];
      this.partTypes[0] = new PartType("introduction", this.corpus, "context");
      this.partTypes[1] = new PartType("methodology", this.corpus, "material");
      this.partTypes[2] = new PartType("results", this.corpus);
      this.partTypes[3] = new PartType("discussion", this.corpus);
      this.partTypes[4] = new PartType("conclusion", this.corpus);
      return true;
   }

   private ScrollPane getControlPane() {
      tfMinSize = new TextField(String.valueOf(CRViewer.CHAIN_MIN_SIZE));
      tfRefName = new TextField("");
      cboRefName = new ComboBox<>();
      cboRefName.getItems().add("");
      cboRefName.getItems().addAll(this.corpus.getAllRefNames());
      cboRefName.setValue("");
      tfTextId = new TextField("");
      ctrlProperties = new PropertyControlPair[CRViewer.PROPERTY_CONTROL_NUMBER];
      for (int i = 0; i < ctrlProperties.length; i++) {
         ctrlProperties[i] = new PropertyControlPair();
      }
      for (PropertyControlPair pair : ctrlProperties) {
         for (String s : this.corpus.getPropertyList()) {
            pair.cboProp.getItems().add(s);
            if (pair.cboProp.getValue() == null) pair.cboProp.setValue(s);
         }
      }
      cboSplitBy = new ComboBox<>();
      for (SplitByType t : SplitByType.values()) {
         cboSplitBy.getItems().add(t.toString());
         if (cboSplitBy.getValue() == null) cboSplitBy.setValue(t.toString());
      }
      cboDisplayUnit = new ComboBox<>();
      for (DisplayUnit t : DisplayUnit.values()) {
         cboDisplayUnit.getItems().add(t.toString());
         if (cboDisplayUnit.getValue() == null) cboDisplayUnit.setValue(t.toString());
      }
      cboDisplayType = new ComboBox<>();
      for (DisplayType t : DisplayType.values()) {
         cboDisplayType.getItems().add(t.toString());
         if (cboDisplayType.getValue() == null) cboDisplayType.setValue(t.toString());
      }
      tfDisplayFilter = new TextField("");
      cboXProperty = new ComboBox<>();
      cboXProperty.getItems().addAll(this.corpus.getPropertyList());
      if (cboXProperty.getItems().size() > 0) cboXProperty.setValue(cboXProperty.getItems().get(0));
      cboYStat = new ComboBox<>();
      for (StatsBar t : StatsBar.values()) {
         cboYStat.getItems().add(t.toString());
         if (cboYStat.getValue() == null) cboYStat.setValue(t.toString());
      }
      cboStabCoeffProperty = new ComboBox<>();
      cboStabCoeffProperty.getItems().addAll(this.corpus.getPropertyList());
      if (cboStabCoeffProperty.getItems().size() > 0) cboStabCoeffProperty.setValue(cboStabCoeffProperty.getItems().get(0));
      tfStabCoeffValue = new TextField("");
      tfContextWidth = new TextField(String.valueOf(CRViewer.CONTEXT_WIDTH));

      Button btnUpdate = new Button("Update!");
      btnUpdate.setOnAction(new EventHandler<ActionEvent>() {
         @Override
         public void handle(ActionEvent event) {
            UpdateDisplay();
         }
      });
      
      GridPane selectPane = new GridPane();
      selectPane.setHgap(10);
      selectPane.setVgap(10);
      //selectPane.setPadding(new Insets(10, 10, 10, 10));
      int row = 1;
      selectPane.add(new Label("Min size: "), 1, row);
      selectPane.add(tfMinSize, 2, row++);
      selectPane.add(new Label("Refname: "), 1, row);
      selectPane.add(tfRefName, 2, row++);
      selectPane.add(new Label("Refname: "), 1, row);
      selectPane.add(cboRefName, 2, row++);
      selectPane.add(new Label("Full id: "), 1, row);
      selectPane.add(tfTextId, 2, row++);
      for (PropertyControlPair pair : ctrlProperties) {
         selectPane.add(pair.cboProp, 1, row);
         selectPane.add(pair.tfProp, 2, row++);
      }
      selectPane.add(new Label("Split by: "), 1, row);
      selectPane.add(cboSplitBy, 2, row++);
      
      GridPane displayPane = new GridPane();
      row = 1;
      displayPane.setHgap(10);
      displayPane.setVgap(10);
      //displayPane.setPadding(new Insets(10, 10, 10, 10));
      displayPane.add(new Label("Display: "), 1, row);
      displayPane.add(cboDisplayType, 2, row++);
      displayPane.add(new Label("Unit: "), 1, row);
      displayPane.add(cboDisplayUnit, 2, row++);
      displayPane.add(new Label("X Property: "), 1, row);
      displayPane.add(cboXProperty, 2, row++);
      displayPane.add(new Label("Y Stat: "), 1, row);
      displayPane.add(cboYStat, 2, row++);
      displayPane.add(new Label("Display Filter: "), 1, row);
      displayPane.add(tfDisplayFilter, 2, row++);
      displayPane.add(new Label("Stab coeff prop: "), 1, row);
      displayPane.add(cboStabCoeffProperty, 2, row++);
      displayPane.add(new Label("Stab coeff value: "), 1, row);
      displayPane.add(tfStabCoeffValue, 2, row++);
      displayPane.add(new Label("Context width: "), 1, row);
      displayPane.add(tfContextWidth, 2, row++);
      
      Label selectionLabel = new Label("=== SELECT OPTIONS ===");
      selectionLabel.setPadding(new Insets(10, 0, 0, 0));
      Label displayLabel = new Label("=== DISPLAY OPTIONS ===");
      displayLabel.setPadding(new Insets(10, 0, 0, 0));
      TextArea licenseBox = new TextArea();
      licenseBox.setText("CRViewer -- (C) 2016-2017 Bruno Oberlé.\nThis program "
            +"is distributed under the terms of the Mozilla Public License, v.2.0. "
            +"This program comes with ABSOLUTELY NO WARRANTY, see the license for more details. "
            +"Source code may be found at boberle.com.");
      licenseBox.setEditable(false);
      licenseBox.setWrapText(true);
      
      //TitledPane selectTitledPane = new TitledPane("Selection options", selectPane);
      //TitledPane displayTitledPane = new TitledPane("Display options", displayPane);
      VBox vbox = new VBox(10);
      vbox.setPadding(new Insets(10, 10, 10, 10));
      vbox.getChildren().addAll(selectionLabel, selectPane, displayLabel, displayPane,
            btnUpdate, licenseBox);
      ScrollPane scrollPane = new ScrollPane();
      scrollPane.setFitToWidth(true);
      scrollPane.setContent(vbox);
      return scrollPane;
   }

   private void setupStage(Stage stage) {
      stage.setTitle("CR Viewer");
      rootpane = new SplitPane(this.getControlPane(), new FlowPane());
      rootpane.setDividerPosition(0, .2);
      Scene scene = new Scene(rootpane);
      stage.setScene(scene);
      stage.setMaximized(true);
   }

   private ChainFilter buildChainFilter() {
      int minSize;
      try {
         minSize = Integer.valueOf(tfMinSize.getText());
      } catch (NumberFormatException e) {
         minSize = CRViewer.CHAIN_MIN_SIZE;
         tfMinSize.setText(String.valueOf(CRViewer.CHAIN_MIN_SIZE));
      }
      ChainFilter filter = new ChainFilter(minSize);
      if (!tfTextId.getText().equals("")) filter.setTextId(tfTextId.getText());
      if (!cboRefName.getValue().equals("")) {
         filter.setRefname(cboRefName.getValue());
      } else if (!tfRefName.getText().equals("")) {
         filter.setRefname(tfRefName.getText());
      }
      for (PropertyControlPair pair : ctrlProperties) {
         if (!pair.tfProp.getText().equals("")) {
            filter.addPropertyFilter(pair.cboProp.getValue(), pair.tfProp.getText());
         }
      }
      return filter;
   }
   
   private void UpdateDisplay() {
      // set the global values
      Chain.stabilityCoeffProperty = cboStabCoeffProperty.getValue();
      try {
         Chain.stabilityCoeffValue = Pattern.compile(tfStabCoeffValue.getText());
      } catch (PatternSyntaxException e) {
         Chain.stabilityCoeffValue = Pattern.compile("");
         tfStabCoeffValue.setText("");
      }
      // build the filter
      ChainFilter filter = buildChainFilter();
      SplitByType splitBy = SplitByType.valueOf(cboSplitBy.getValue());
      ChainCollection chainColl = new ChainCollection(corpus, splitBy, filter);
      /*System.out.println("List of chains:");
      for (Chain chain : chainColl.getChains()) {
         chain.speak();
      }*/
      DisplayType displayType = DisplayType.valueOf(cboDisplayType.getValue());
      DisplayUnit displayUnit = DisplayUnit.valueOf(cboDisplayUnit.getValue());
      Pane pane = null;
      if (displayType.equals(DisplayType.stats)) {
         if (displayUnit.equals(DisplayUnit.chain)) {
            pane = getChainStatsPane(chainColl);
         } else {
            pane = getChunkStatsPane(chainColl, displayUnit);
         }
      } else if (displayType.equals(DisplayType.frequencies)) {
         pane = getFreqPane(chainColl, displayUnit);
      } else if (displayType.equals(DisplayType.pie)) {
         pane = getFreqPiePane(chainColl);
      } else if (displayType.equals(DisplayType.concordancer)) {
         pane = getConcordancerPane(chainColl);
      } else if (displayType.equals(DisplayType.statsBar)) {
         pane = getStatsBarPane(chainColl, displayUnit);
      } else {
         throw new NotImplementedException();
      }
      if (rootpane.getItems().size() > 1) {
         rootpane.getItems().set(rootpane.getItems().size()-1, pane);
      } else {
         rootpane.getItems().add(pane);
      }
   }

   private Pane getFreqPane(ChainCollection chainColl, DisplayUnit displayUnit) {
      if (displayUnit.equals(DisplayUnit.chain)) {
         return getFreqPerChainPane(chainColl);
      } else {
         return getFreqPerChunkPane(chainColl, displayUnit);
      }
   }

   private Pane getFreqPerChainPane(ChainCollection chainColl) {
      TableView<Chain> table = new TableView<Chain>();
      table.getItems().addAll(chainColl.getChains());
      table.getColumns().add(new ShortCol<Chain,String>("Id", "FullId").col);
      for (String propName : chainColl.getPropertyList()) {
         for (String propValue : chainColl.getPropertyMap().get(propName)) {
            table.getColumns().add(new ShortCol<Chain,Integer>(propName + ":\n" + propValue, "getPropertyFreq", propName, propValue).col);
         }
      }
      CRViewer.writeTsv(CRViewer.getTsvFromTableView(table), "output_frequencies");
      Pane pane = new StackPane(table);
      //Pane pane = new VBox(table);
      pane.setPadding(new Insets(20));
      //VBox.setVgrow(table, Priority.ALWAYS);
      return pane;
   }
   
   private void populateTableWithChunk(TableView<TextChunk> table, DisplayUnit displayUnit) {
      if (displayUnit.equals(DisplayUnit.paragraph)) {
         table.getItems().addAll(this.corpus.getParagraphs());
      } else if (displayUnit.equals(DisplayUnit.part)) {
         table.getItems().addAll(this.corpus.getParts());
      } else if (displayUnit.equals(DisplayUnit.text)) {
         table.getItems().addAll(this.corpus.getTexts());
      } else if (displayUnit.equals(DisplayUnit.corpus)) {
         table.getItems().addAll(this.corpus);
      } else if (displayUnit.equals(DisplayUnit.parttype)) {
         table.getItems().addAll(this.partTypes);
      } else {
         throw new NotImplementedException();
      }
   }

   private Pane getFreqPerChunkPane(ChainCollection chainColl, DisplayUnit displayUnit) {
      TableView<TextChunk> table = new TableView<TextChunk>();
      this.populateTableWithChunk(table, displayUnit);
      table.getColumns().add(new ShortCol<TextChunk,String>("Id", "Id").col);
      for (String propName : chainColl.getPropertyList()) {
         for (String propValue : chainColl.getPropertyMap().get(propName)) {
            //TODO: wrap the header text if it is too large
            table.getColumns().add(new ShortCol<TextChunk,Integer>(propName + ":\n" + propValue, "getPropertyFreq", propName, propValue, chainColl).col);
         }
      }
      CRViewer.writeTsv(CRViewer.getTsvFromTableView(table), "output_frequencies");
      Pane pane = new StackPane(table);
      //Pane pane = new VBox(table);
      pane.setPadding(new Insets(20));
      //VBox.setVgrow(table, Priority.ALWAYS);
      return pane;
   }

   private Pane getFreqPiePane(ChainCollection chainColl) {
      Pane pane;
      String propName = this.cboXProperty.getValue();
      StringBuilder tsv = new StringBuilder();
      if (chainColl.getPropertyMap().containsKey(propName)) {
         PieChart chart = new PieChart();
         chart.setLegendSide(Side.BOTTOM);
         double total = this.corpus.getPropertyFreq(propName, chainColl);
         ArrayList<String> valueList = chainColl.getPropertyMap().get(propName); 
         Collections.sort(valueList);
         for (String propValue : valueList) {
            double value = this.corpus.getPropertyFreq(propName, propValue, chainColl);
            String label = propValue + " (" + String.format("%.1f%%", value/total*100) + ")";
            chart.getData().add(new Data(label, value));
            tsv.append(propValue + "\t" + String.format("%.3f", value/total*100) + "\n");
         }
         for (Data d : chart.getData()) {
            Tooltip tt = new Tooltip(String.format("%.2f%%", d.getPieValue()/total*100));
            Tooltip.install(d.getNode(), tt);
         }
         CRViewer.writeTsv(tsv.toString(), "output_pie_freq_"+propName);
         CRViewer.writeChart(chart, "output_pie_"+propName);
         pane = new StackPane(chart);
         pane.setPadding(new Insets(20));
         //pane = new VBox(chart);
         //VBox.setVgrow(chart, Priority.ALWAYS);
      } else {
         pane = new VBox(new Label("No data."));
         pane.setPadding(new Insets(20));
      }
      return pane;
   }

   @SuppressWarnings("unchecked")
   private Pane getChunkStatsPane(ChainCollection chainColl, DisplayUnit displayUnit) {
      TableView<TextChunk> table = new TableView<TextChunk>();
      this.populateTableWithChunk(table, displayUnit);
      table.getColumns().addAll(
            new ShortCol<TextChunk,String>("Id", "Id").col,
            new ShortCol<TextChunk,Double>("AvgL2LDist", "AverageLinkToLinkDistance", chainColl).col,
            new ShortCol<TextChunk,Double>("AvgLinkLength", "AverageLinkLength", chainColl).col,
            new ShortCol<TextChunk,Double>("AvgChainSize", "AverageChainSize", chainColl).col,
            new ShortCol<TextChunk,Integer>("ChainCount", "ChainCount", chainColl).col,
            new ShortCol<TextChunk,Integer>("LinkCount", "LinkCount", chainColl).col,
            new ShortCol<TextChunk,Double>("Stab Coeff", "StabilityCoeff", chainColl).col,
            new ShortCol<TextChunk,Double>("ChainDensity", "ChainDensity", chainColl).col,
            new ShortCol<TextChunk,Double>("LinkDensity", "LinkDensity", chainColl).col,
            new ShortCol<TextChunk,Double>("AnnotationDensity", "AnnotationDensity").col,
            new ShortCol<TextChunk,Double>("#tokens", "WordTokenCount").col);
      if (displayUnit.equals(DisplayUnit.part) || displayUnit.equals(DisplayUnit.parttype)
            || displayUnit.equals(DisplayUnit.text) || displayUnit.equals(DisplayUnit.corpus)) {
         //table.getColumns().add(new ShortCol<TextChunk,Double>("#pars", "ParagraphsCount").col);
         TableColumn<TextChunk,Integer> col = new TableColumn<TextChunk,Integer>("#pars");
         col.setSortable(true);
         col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TextChunk,Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<TextChunk, Integer> p) {
               // p.getValue() returns the Person/whatever instance for a particular TableView row
               return new ReadOnlyObjectWrapper<Integer>(((HasParagraphs)p.getValue()).getParagraphCount());
            }
         });
         table.getColumns().add(col);
      }
      if (displayUnit.equals(DisplayUnit.text) || displayUnit.equals(DisplayUnit.corpus)) {
         //table.getColumns().add(new ShortCol<TextChunk,Double>("#pars", "ParagraphsCount").col);
         TableColumn<TextChunk,Integer> col = new TableColumn<TextChunk,Integer>("#parts");
         col.setSortable(true);
         col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TextChunk,Integer>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(CellDataFeatures<TextChunk, Integer> p) {
               // p.getValue() returns the Person/whatever instance for a particular TableView row
               return new ReadOnlyObjectWrapper<Integer>(((HasParts)p.getValue()).getPartCount());
            }
         });
         table.getColumns().add(col);
      }
      CRViewer.writeTsv(CRViewer.getTsvFromTableView(table), "output_stats");
      Pane pane = new StackPane(table);
      //Pane pane = new VBox(table);
      pane.setPadding(new Insets(20));
      //VBox.setVgrow(table, Priority.ALWAYS);
      return pane;
   }

   @SuppressWarnings("unchecked")
   private Pane getChainStatsPane(ChainCollection chainColl) {
      TableView<Chain> table = new TableView<Chain>();
      table.getItems().addAll(chainColl.getChains());
      table.getColumns().addAll(
            new ShortCol<Chain,String>("Id", "FullId").col,
            new ShortCol<Chain,Integer>("Size", "Size").col,
            new ShortCol<Chain,Double>("L2LDist", "AverageLinkToLinkDistance").col,
            new ShortCol<Chain,Double>("LinkLen", "AverageLinkLength").col,
            new ShortCol<Chain,Double>("Stab Coeff", "StabilityCoeff").col);
      CRViewer.writeTsv(CRViewer.getTsvFromTableView(table), "output_stats");
      Pane pane = new StackPane(table);
      //Pane pane = new VBox(table);
      pane.setPadding(new Insets(20));
      //VBox.setVgrow(table, Priority.ALWAYS);
      return pane;
   }
   
   private double getStatValue(ChainCollection chainColl, TextChunk chunk) {
      StatsBar statName = StatsBar.valueOf(this.cboYStat.getValue());
      double val = 0;
      if (statName.equals(StatsBar.getAnnotationDensity)) {
         val = (double)chunk.getAnnotationDensity();
      } else if (statName.equals(StatsBar.getAverageLinkToLinkDistance)) {
         val = (double)chunk.getAverageLinkToLinkDistance(chainColl);
      } else if (statName.equals(StatsBar.getAverageLinkLength)) {
         val = (double)chunk.getAverageLinkLength(chainColl);
      } else if (statName.equals(StatsBar.getAverageChainSize)) {
         val = (double)chunk.getAverageChainSize(chainColl);
      } else if (statName.equals(StatsBar.getChainCount)) {
         val = (double)chunk.getChainCount(chainColl);
      } else if (statName.equals(StatsBar.getLinkCount)) {
         val = (double)chunk.getLinkCount(chainColl);
      } else if (statName.equals(StatsBar.getChainDensity)) {
         val = (double)chunk.getChainDensity(chainColl);
      } else if (statName.equals(StatsBar.getLinkDensity)) {
         val = (double)chunk.getLinkDensity(chainColl);
      } else if (statName.equals(StatsBar.getAnnotationDensity)) {
         val = (double)chunk.getAnnotationDensity();
      } else if (statName.equals(StatsBar.getTokenCount)) {
         val = (double)chunk.getTokenCount();
      }
      return val;
   }
   
   private Pane getStatsBarPane(ChainCollection chainColl, DisplayUnit displayUnit) {
      Pane pane;
      String statName = this.cboYStat.getValue();
      Axis<String> xAxis = new CategoryAxis();
      xAxis.setLabel(statName);
      //xAxis.setAutoRanging(true);
      //xAxis.setLowerBound(1900);
      //xAxis.setUpperBound(2300);
      //xAxis.setTickUnit(50);
      NumberAxis yAxis = new NumberAxis();
      //yAxis.setLabel("TODO");
      yAxis.setAutoRanging(true);
      BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
      XYChart.Series<String, Number> seriesStat = new XYChart.Series<>();
      seriesStat.setName(statName);
      ArrayList<? extends TextChunk> list;
      if (displayUnit.equals(DisplayUnit.paragraph)) {
         list = this.corpus.getParagraphs();
      } else if (displayUnit.equals(DisplayUnit.part)) {
         list = this.corpus.getParts();
      } else if (displayUnit.equals(DisplayUnit.text)) {
         list = this.corpus.getTexts();
      } else if (displayUnit.equals(DisplayUnit.corpus)) {
         ArrayList<Corpus> tmp = new ArrayList<Corpus>();
         tmp.add(this.corpus);
         list = tmp;
      } else if (displayUnit.equals(DisplayUnit.parttype)) {
         ArrayList<PartType> tmp = new ArrayList<>();
         for (PartType pt : this.partTypes) {
            tmp.add(pt);
         }
         list = tmp;
      } else {
         throw new NotImplementedException();
      }
      Pattern pattern = null;
      if (!this.tfDisplayFilter.getText().equals("")) {
         pattern = Pattern.compile(this.tfDisplayFilter.getText());
      }
      double max = 0;
      for (TextChunk chunk : list) {
         if (pattern != null && !pattern.matcher(chunk.getId()).find()) {
            continue;
         }
         double val = this.getStatValue(chainColl, chunk);
         if (Double.isNaN(val)) val = 0;
         if (max < val) max = val;
         seriesStat.getData().add(new XYChart.Data<String,Number>(chunk.getId(), val));
      }
      yAxis.setLowerBound(0);
      yAxis.setUpperBound(max);
      chart.getData().add(seriesStat);
      chart.legendVisibleProperty().set(false);
      CRViewer.writeChart(chart, "output_bars_"+statName);
      pane = new StackPane(chart);
      pane.setPadding(new Insets(20));
      return pane;
   }

   /*@SuppressWarnings("unchecked")
   private Pane getStatsChainsPane(ChainCollection chains) {
      TableView<Chain> table = new TableView<Chain>();
      table.getItems().addAll(chains.getChains());
      TableColumn<Chain, String> colFullId = new TableColumn<Chain, String>("Id");
      colFullId.setCellValueFactory(new PropertyValueFactory<Chain, String>("FullId"));
      TableColumn<Chain, Integer> colSize = new TableColumn<Chain, Integer>("Size");
      colSize.setCellValueFactory(new PropertyValueFactory<Chain, Integer>("Size"));
      TableColumn<Chain, Double> colAverageGap = new TableColumn<Chain, Double>("Gap");
      colAverageGap.setCellValueFactory(new PropertyValueFactory<Chain, Double>("AverageGap"));
      TableColumn<Chain, Double> colAverageAnnotationLength = new TableColumn<Chain, Double>("AnnotLen");
      colAverageAnnotationLength.setCellValueFactory(new PropertyValueFactory<Chain, Double>("AverageAnnotationLength"));
      //colAverageAnnotationLength.setCellValueFactory(new CustomPropertyValueFactory<Chain, Double>("AverageAnnotationLength"));
      //TableColumn<Chain, Double> colAverageAnnotationLength = new ShortCol<Chain, Double>("AnnotLen", "AverageAnnotationLength").col;
      table.getColumns().addAll(colFullId, colSize, colAverageGap, colAverageAnnotationLength);
      FlowPane pane = new FlowPane(table);
      return pane;
   }*/
   
   @SuppressWarnings("unchecked")
   private Pane getConcordancerPane(ChainCollection chainColl) {
      try {
         Annotation.contextWidth = Integer.valueOf(tfContextWidth.getText());
      } catch (NumberFormatException e) {
         Annotation.contextWidth = CRViewer.CONTEXT_WIDTH;
         tfContextWidth.setText(String.valueOf(CRViewer.CONTEXT_WIDTH));
      }
      TableView<Annotation> table = new TableView<Annotation>();
      table.getItems().addAll(chainColl.getAnnotations());
      if (Annotation.contextWidth == 0) {
         table.getColumns().addAll(
               new ShortCol<Annotation,String>("Text and Chain", "FullId").col,
               new ShortCol<Annotation,String>("Text", "Text").col);
      } else {
         table.getColumns().addAll(
               new ShortCol<Annotation,String>("Left", "LeftContext").col,
               new ShortCol<Annotation,String>("Text and Right", "TextWithRightContext").col,
               new ShortCol<Annotation,String>("Text and Chain", "FullId").col);
         table.getColumns().get(0).setStyle("-fx-alignment: CENTER-RIGHT;");
      }
      CRViewer.writeTsv(CRViewer.getTsvFromTableView(table), "output_concordance");
      Pane pane = new StackPane(table);
      //Pane pane = new VBox(table);
      pane.setPadding(new Insets(20));
      //VBox.setVgrow(table, Priority.ALWAYS);
      return pane;
   }

   @Override
   public void start(Stage primaryStage) throws IOException,ParseException {
      boolean mustClose = !this.createCorpus(primaryStage);
      if (mustClose) {
         System.out.println("No file selected. Closing.");
         //primaryStage.close(); // doesn't work (not shown?), use Platform.exit()
         Platform.exit();
         return;
      }
      this.primaryStage = primaryStage;
      this.setupStage(primaryStage);
      primaryStage.show();
   }

   public static void main(String[] args) {
      Application.launch(args);
   }   
}


class ShortCol<T,V> {
   public TableColumn<T, V> col;
   private void createCol(String heading) {
      this.col = new TableColumn<T, V>(heading);
      this.col.setSortable(true);
   }
   public ShortCol(String heading, String methodName) {
      createCol(heading);
      this.col.setCellValueFactory(new CustomPropertyValueFactory<T, V>(methodName));
   }
   public ShortCol(String heading, String methodName, String propName, String propValue) {
      createCol(heading);
      this.col.setCellValueFactory(new CustomPropertyValueFactoryForChainFreq<T, V>(methodName, propName, propValue));
   }
   public ShortCol(String heading, String methodName, String propName, String propValue, ChainCollection chainColl) {
      createCol(heading);
      this.col.setCellValueFactory(new CustomPropertyValueFactoryForChunkFreq<T, V>(methodName, propName, propValue, chainColl));
   }
   public ShortCol(String heading, String methodName, ChainCollection chainColl) {
      createCol(heading);
      this.col.setCellValueFactory(new CustomPropertyValueFactoryForChunkStats<T, V>(methodName, chainColl));
   }
}


class CustomPropertyValueFactory<T,V> implements Callback<TableColumn.CellDataFeatures<T,V>, ObservableValue<V>> {
   protected String methodName;
   public CustomPropertyValueFactory(String methodName) {
      this.methodName = methodName;
   }
   protected Method tryMethodFromName(T c, String methodName) throws NoSuchMethodException, SecurityException {
      return c.getClass().getMethod(methodName);
   }
   protected Method getMethodFromName(T c) {
      try {
         return tryMethodFromName(c, this.methodName);
      } catch (NoSuchMethodException e) {
         try {
            return tryMethodFromName(c, "get"+this.methodName);
         } catch (NoSuchMethodException | SecurityException e1) {
            e1.printStackTrace();
         }
      } catch (SecurityException e) {
         e.printStackTrace();
      }
      System.out.println("can't find method '"+this.methodName+"'");
      System.exit(1);
      return null;
   }
   @SuppressWarnings("unchecked")
   protected V invokeMethod(Method method, T obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return (V)method.invoke(obj);
   }
   @Override
   public ObservableValue<V> call(CellDataFeatures<T, V> p) {
      // p.getValue() returns the Person/whatever instance for a particular TableView row
      try {
         T obj = p.getValue();
         Method method = getMethodFromName(obj);
         return new ReadOnlyObjectWrapper<V>(this.invokeMethod(method, obj));
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         e.printStackTrace();
      }
      System.out.println("can't invoke method '"+this.methodName+"'");
      System.exit(1);
      return null;
   }
}

class CustomPropertyValueFactoryForChainFreq<T,V> extends CustomPropertyValueFactory<T,V> {
   protected String propName;
   protected String propValue;
   protected CustomPropertyValueFactoryForChainFreq(String methodName, String propName, String propValue) {
      super(methodName);
      this.propName = propName;
      this.propValue = propValue;
   }
   protected Method tryMethodFromName(T c, String methodName) throws NoSuchMethodException, SecurityException {
      return c.getClass().getMethod(methodName, String.class, String.class);
   }
   @SuppressWarnings("unchecked")
   public V invokeMethod(Method method, T obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return (V)method.invoke(obj, this.propName, this.propValue);
   }
}

class CustomPropertyValueFactoryForChunkFreq<T,V> extends CustomPropertyValueFactoryForChainFreq<T,V> {
   protected ChainCollection chainColl;
   protected CustomPropertyValueFactoryForChunkFreq(String methodName, String propName, String propValue, ChainCollection chainColl) {
      super(methodName, propName, propValue);
      this.chainColl = chainColl;
   }
   protected Method tryMethodFromName(T c, String methodName) throws NoSuchMethodException, SecurityException {
      return c.getClass().getMethod(methodName, String.class, String.class, ChainCollection.class);
   }
   @SuppressWarnings("unchecked")
   public V invokeMethod(Method method, T obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return (V)method.invoke(obj, this.propName, this.propValue, this.chainColl);
   }
}

class CustomPropertyValueFactoryForChunkStats<T,V> extends CustomPropertyValueFactory<T,V> {
   protected ChainCollection chainColl;
   protected CustomPropertyValueFactoryForChunkStats(String methodName, ChainCollection chainColl) {
      super(methodName);
      this.chainColl = chainColl;
   }
   protected Method tryMethodFromName(T c, String methodName) throws NoSuchMethodException, SecurityException {
      return c.getClass().getMethod(methodName, ChainCollection.class);
   }
   @SuppressWarnings("unchecked")
   public V invokeMethod(Method method, T obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return (V)method.invoke(obj, this.chainColl);
   }
}

/*
 * class CustomPropertyValueFactory<T,V> implements Callback<TableColumn.CellDataFeatures<T,V>, ObservableValue<V>> {
   private String methodName;
   private String methodArgument;
   private boolean useArgument;
   public CustomPropertyValueFactory(String methodName) {
      this(methodName, null);
   }
   public CustomPropertyValueFactory(String methodName, String methodArgument) {
      this.methodName = methodName;
      this.methodArgument = methodArgument;
      this.useArgument = methodArgument!=null;
   }
   private Method tryMethodFromName(T c, String methodName) throws NoSuchMethodException, SecurityException {
      if (this.useArgument) {
         return c.getClass().getMethod(methodName, this.methodArgument.getClass());
      }
      return c.getClass().getMethod(methodName);
   }
   private Method getMethodFromName(T c) {
      try {
         return tryMethodFromName(c, this.methodName);
      } catch (NoSuchMethodException e) {
         try {
            return tryMethodFromName(c, "get"+this.methodName);
         } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
         } catch (SecurityException e1) {
            e1.printStackTrace();
         }
      } catch (SecurityException e) {
         e.printStackTrace();
      }
      return null;
   }
   @Override
   public ObservableValue<V> call(CellDataFeatures<T, V> p) {
      // p.getValue() returns the Person/whatever instance for a particular TableView row
      try {
         T obj = p.getValue();
         Method method = getMethodFromName(obj);
         if (this.useArgument) {
            return new ReadOnlyObjectWrapper<V>((V)method.invoke(obj, this.methodArgument));
         } else {
            return new ReadOnlyObjectWrapper<V>((V)method.invoke(obj));
         }
      } catch (IllegalAccessException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IllegalArgumentException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (InvocationTargetException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return null;
   }
}
 */


