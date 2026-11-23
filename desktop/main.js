// TR-25 Bill Generator – Windows desktop app (works fully offline)
const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const { pathToFileURL } = require('url');

let win;
const appDir = path.join(__dirname, 'app');

function createWindow() {
  win = new BrowserWindow({
    width: 1220, height: 860, minWidth: 820, minHeight: 600,
    title: 'TR-25 Bill Generator',
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false },
  });
  win.removeMenu();
  win.loadFile(path.join(appDir, 'index.html'));
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());

// Report HTML -> real A4 PDF file (portrait or landscape), then opens it.
ipcMain.handle('pdf', async (_e, { html, name, landscape }) => {
  const r = await dialog.showSaveDialog(win, { defaultPath: name, filters: [{ name: 'PDF', extensions: ['pdf'] }] });
  if (r.canceled || !r.filePath) return 'Cancelled';
  const w = new BrowserWindow({ show: false, webPreferences: { offscreen: true } });
  try {
    await w.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(html),
      { baseURLForDataURL: pathToFileURL(appDir).href + '/' });
    await w.webContents.executeJavaScript('document.fonts.ready.then(() => true)');
    const pdf = await w.webContents.printToPDF({ pageSize: 'A4', landscape: !!landscape, printBackground: true, preferCSSPageSize: true });
    fs.writeFileSync(r.filePath, pdf);
  } finally {
    w.destroy();
  }
  shell.openPath(r.filePath);
  return 'Saved ' + path.basename(r.filePath);
});

// Word (.doc), CSV and backup files.
ipcMain.handle('save', async (_e, { name, text }) => {
  const ext = path.extname(name).slice(1) || 'txt';
  const r = await dialog.showSaveDialog(win, { defaultPath: name, filters: [{ name: ext.toUpperCase(), extensions: [ext] }] });
  if (r.canceled || !r.filePath) return 'Cancelled';
  fs.writeFileSync(r.filePath, text, 'utf8');
  return 'Saved ' + path.basename(r.filePath);
});
