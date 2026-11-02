package noxisclient.mod.modules.impl.client;

public enum FontMode {
   DEFAULT(true, "default.ttf"),
   MISANS(true, "misans-regular.ttf"),
   BOREL(true, "borel-regular.ttf"),
   HOPRC(true, "hoprc.ttf"),
   HYW(true, "hyw.ttf"),
   SHORELINE(true, "verdana.ttf", "assets/noxisclient/font/verdana.ttf"),
   SYSTEM(true, null),
   MINECRAFT(false, null),
   CUSTOM_OTF(true, "font.otf");

   private static final String FONT_DIR = "assets/alienclient/font/";
   private final boolean custom;
   private final String fontFile;
   private final String resourcePath;

   private FontMode(boolean custom, String fontFile) {
      this(custom, fontFile, fontFile == null ? null : "assets/alienclient/font/" + fontFile);
   }

   private FontMode(boolean custom, String fontFile, String resourcePath) {
      this.custom = custom;
      this.fontFile = fontFile;
      this.resourcePath = resourcePath;
   }

   public boolean isCustom() {
      return this.custom;
   }

   public boolean hasBundledFont() {
      return this.fontFile != null;
   }

   public String getFontFile() {
      return this.fontFile == null ? DEFAULT.fontFile : this.fontFile;
   }

   public String getResourcePath() {
      return this.resourcePath == null ? DEFAULT.resourcePath : this.resourcePath;
   }
}
