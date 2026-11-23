const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('desktop', {
  pdf: (html, name, landscape) => ipcRenderer.invoke('pdf', { html, name, landscape }),
  save: (name, text) => ipcRenderer.invoke('save', { name, text }),
});
