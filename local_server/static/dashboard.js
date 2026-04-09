const clientsList = document.getElementById("clients-list");
const clientName = document.getElementById("client-name");
const clientMeta = document.getElementById("client-meta");
const clientDetails = document.getElementById("client-details");
const connectionPill = document.getElementById("connection-pill");
const refreshButton = document.getElementById("refresh-button");
const openMapsLink = document.getElementById("open-maps-link");

let selectedClientId = null;
let clientsCache = [];
let historyPolyline = null;
let marker = null;

const map = L.map("map", {
    zoomControl: true,
}).setView([40.4168, -3.7038], 5);

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

refreshButton?.addEventListener("click", () => {
    void refreshDashboard();
});

function setConnectionStatus(text, ok = true) {
    if (!connectionPill) {
        return;
    }
    connectionPill.textContent = text;
    connectionPill.style.background = ok ? "#e7f5e7" : "#fff2de";
    connectionPill.style.color = ok ? "#2f7a39" : "#a76712";
}

function formatDate(dateText) {
    if (!dateText) {
        return "Sin datos";
    }

    const date = new Date(dateText);
    if (Number.isNaN(date.getTime())) {
        return dateText;
    }

    return new Intl.DateTimeFormat("es-ES", {
        dateStyle: "short",
        timeStyle: "medium",
    }).format(date);
}

function formatRelativeTime(dateText) {
    if (!dateText) {
        return "Sin conexión todavía";
    }

    const date = new Date(dateText);
    const diffMs = Date.now() - date.getTime();
    const diffSeconds = Math.round(diffMs / 1000);

    if (Math.abs(diffSeconds) < 60) {
        return "Hace unos segundos";
    }
    if (Math.abs(diffSeconds) < 3600) {
        return `Hace ${Math.round(diffSeconds / 60)} min`;
    }
    if (Math.abs(diffSeconds) < 86400) {
        return `Hace ${Math.round(diffSeconds / 3600)} h`;
    }
    return formatDate(dateText);
}

function renderClients(clients) {
    clientsCache = clients;
    if (!clientsList) {
        return;
    }

    if (!clients.length) {
        clientsList.innerHTML = '<div class="empty-state">Todavía no hay clientes registrados.</div>';
        clientName.textContent = "Sin clientes";
        clientMeta.textContent = "Cuando un teléfono cliente se registre, aparecerá aquí automáticamente.";
        clientDetails.innerHTML = "";
        openMapsLink?.classList.add("hidden");
        clearMap();
        return;
    }

    if (!selectedClientId || !clients.some((client) => client.id === selectedClientId)) {
        selectedClientId = clients[0].id;
    }

    clientsList.innerHTML = "";
    for (const client of clients) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `client-card${client.id === selectedClientId ? " active" : ""}`;
        button.innerHTML = `
            <div class="client-card-row">
                <h3>${escapeHtml(client.display_name)}</h3>
                <span class="status-dot ${client.is_online ? "online" : "offline"}"></span>
            </div>
            <p>${escapeHtml(client.device_model || "Android")} · ${escapeHtml(formatRelativeTime(client.last_seen_at))}</p>
        `;
        button.addEventListener("click", () => {
            selectedClientId = client.id;
            renderClients(clientsCache);
            void loadClientDetails(selectedClientId);
        });
        clientsList.appendChild(button);
    }
}

async function refreshDashboard() {
    try {
        const response = await fetch("/api/clients", { cache: "no-store" });
        if (!response.ok) {
            throw new Error("No se pudo cargar la lista de clientes");
        }

        const clients = await response.json();
        renderClients(clients);
        setConnectionStatus("Conectado", true);

        if (selectedClientId) {
            await loadClientDetails(selectedClientId);
        }
    } catch (error) {
        setConnectionStatus("Servidor no disponible", false);
        console.error(error);
    }
}

async function loadClientDetails(clientId) {
    if (!clientId) {
        return;
    }

    try {
        const response = await fetch(`/api/clients/${encodeURIComponent(clientId)}?limit=100`, {
            cache: "no-store",
        });
        if (!response.ok) {
            throw new Error("No se pudo cargar el detalle del cliente");
        }

        const payload = await response.json();
        renderClientDetails(payload.client, payload.history || []);
    } catch (error) {
        console.error(error);
    }
}

function renderClientDetails(client, history) {
    clientName.textContent = client.display_name || "Cliente";
    clientMeta.textContent = `${client.device_model || "Android"} · ${formatRelativeTime(client.last_seen_at)}`;

    const coords = client.last_latitude != null && client.last_longitude != null
        ? `${client.last_latitude.toFixed(6)}, ${client.last_longitude.toFixed(6)}`
        : "Sin posición";
    const accuracy = client.last_accuracy != null
        ? `${Math.round(client.last_accuracy)} m`
        : "Sin dato";
    const battery = client.last_battery_percent != null
        ? `${client.last_battery_percent}%${client.last_is_charging ? " · cargando" : ""}`
        : "Sin dato";

    clientDetails.innerHTML = `
        <div>
            <dt>Cliente</dt>
            <dd>${escapeHtml(client.id)}</dd>
        </div>
        <div>
            <dt>Última conexión</dt>
            <dd>${escapeHtml(formatDate(client.last_seen_at))}</dd>
        </div>
        <div>
            <dt>Coordenadas</dt>
            <dd>${escapeHtml(coords)}</dd>
        </div>
        <div>
            <dt>Precisión</dt>
            <dd>${escapeHtml(accuracy)}</dd>
        </div>
        <div>
            <dt>Batería</dt>
            <dd>${escapeHtml(battery)}</dd>
        </div>
        <div>
            <dt>Histórico cargado</dt>
            <dd>${history.length} puntos</dd>
        </div>
    `;

    if (client.last_latitude != null && client.last_longitude != null) {
        const externalUrl = `https://www.openstreetmap.org/?mlat=${client.last_latitude}&mlon=${client.last_longitude}#map=17/${client.last_latitude}/${client.last_longitude}`;
        openMapsLink.href = externalUrl;
        openMapsLink.classList.remove("hidden");
    } else {
        openMapsLink.classList.add("hidden");
    }

    updateMap(client, history);
}

function updateMap(client, history) {
    if (historyPolyline) {
        map.removeLayer(historyPolyline);
        historyPolyline = null;
    }
    if (marker) {
        map.removeLayer(marker);
        marker = null;
    }

    const points = history
        .filter((point) => point.latitude != null && point.longitude != null)
        .map((point) => [point.latitude, point.longitude]);

    if (!points.length && (client.last_latitude == null || client.last_longitude == null)) {
        clearMap();
        return;
    }

    const lastPoint = points.length
        ? points[points.length - 1]
        : [client.last_latitude, client.last_longitude];

    if (points.length > 1) {
        historyPolyline = L.polyline(points, {
            color: "#3fa34d",
            weight: 4,
            opacity: 0.75,
        }).addTo(map);
    }

    marker = L.marker(lastPoint).addTo(map);
    marker.bindPopup(`
        <strong>${escapeHtml(client.display_name || "Cliente")}</strong><br>
        ${escapeHtml(formatDate(client.last_seen_at))}
    `);

    const bounds = L.latLngBounds(points.length ? points : [lastPoint]);
    map.fitBounds(bounds.pad(0.2));
}

function clearMap() {
    if (historyPolyline) {
        map.removeLayer(historyPolyline);
        historyPolyline = null;
    }
    if (marker) {
        map.removeLayer(marker);
        marker = null;
    }
    map.setView([40.4168, -3.7038], 5);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

void refreshDashboard();
window.setInterval(() => {
    void refreshDashboard();
}, 5000);
