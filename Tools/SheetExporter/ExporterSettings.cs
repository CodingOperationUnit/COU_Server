using System;
using System.IO;
using Newtonsoft.Json;

namespace COU.SheetExporter
{
    // 마지막 입력값. 사용자별 %LocalAppData%\COU\SheetExporter에 저장한다.
    internal sealed class ExporterSettings
    {
        private static readonly string FilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "COU", "SheetExporter", "settings.json");

        public string SaveFolder = "";
        public string SpreadsheetUrl = "https://docs.google.com/spreadsheets/d/1llhByRO8ehiFglekMHbZ2tR6Daxt1rds1hmcOdp007k/edit";
        public string ApiUrl = "https://script.google.com/macros/s/AKfycbyuaovF1tA7_YEbnI4Rx_5meIPXOG5sdxsRYy3jezsDFiD1Up6U7LuCDO8srIzabK75eA/exec";
        public string ClientFolder = "";
        public bool ClientCopy;

        public static ExporterSettings Load() => File.Exists(FilePath)
            ? JsonConvert.DeserializeObject<ExporterSettings>(File.ReadAllText(FilePath))
            : new ExporterSettings();

        public void Save()
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FilePath));
            File.WriteAllText(FilePath, JsonConvert.SerializeObject(this, Formatting.Indented));
        }
    }
}
