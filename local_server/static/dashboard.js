const clientsList = document.getElementById("clients-list");
const clientName = document.getElementById("client-name");
const clientMeta = document.getElementById("client-meta");
const connectionPill = document.getElementById("connection-pill");
const refreshButton = document.getElementById("refresh-button");
const openMapsLink = document.getElementById("open-maps-link");
const clearHistoryButton = document.getElementById("clear-history-button");
const deleteClientButton = document.getElementById("delete-client-button");
const clientForm = document.getElementById("client-form");
const displayNameInput = document.getElementById("client-display-name-input");
const ageInput = document.getElementById("client-age-input");
const formStatus = document.getElementById("form-status");
const historyTableBody = document.getElementById("history-table-body");
const recentActivity = document.getElementById("recent-activity");
const searchInput = document.getElementById("client-search");
const filterRow = document.getElementById("filter-row");

const statTotalClients = document.getElementById("stat-total-clients");
const statNewestClient = document.getElementById("stat-newest-client");
const statOnlineClients = document.getElementById("stat-online-clients");
const statOfflineClients = document.getElementById("stat-offline-clients");
const statTotalLocations = document.getElementById("stat-total-locations");
const statLastSync = document.getElementById("stat-last-sync");
const statClientsWithLocation = document.getElementById("stat-clients-with-location");
const statDatabasePath = document.getElementById("stat-database-path");

const detailCoordinates = document.getElementById("detail-coordinates");
const detailAccuracy = document.getElementById("detail-accuracy");
const detailBattery = document.getElementById("detail-battery");
const detailHistory = document.getElementById("detail-history");

const state = {
    selectedClientId: null,
    clients: [],
    selectedClient: null,
    selectedHistory: [],
    selectedMetrics: null,
    searchQuery: "",
    filter: "all",
};

let historyPolyline = null;
let marker = null;

class LoginRequiredError extends Error {
    constructor(message = "Necesitas iniciar sesión como administrador.") {
        super(message);
        this.name = "LoginRequiredError";
    }
}

const map = L.map("map", {
    zoomControl: true,
}).setView([40.4168, -3.7038], 5);

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

function redirectToLogin() {
    const next = `${window.location.pathname}${window.location.search}`;
    window.location.href = `/login?next=${encodeURIComponent(next)}`;
}

async function fetchJson(url, options) {
    const response = await fetch(url, options);
    let payload = null;

    try {
        payload = await response.json();
    } catch (error) {
        payload = null;
    }

    if (response.status === 401) {
        redirectToLogin();
        throw new LoginRequiredError(
            payload?.error || "Necesitas iniciar sesión como administrador."
        );
    }

    return { response, payload };
}

refreshButton?.addEventListener("click", () => {
    void refreshDashboard();
});

searchInput?.addEventListener("input", (event) => {
    state.searchQuery = event.target.value.trim().toLowerCase();
    renderClients();
});

filterRow?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-filter]");
    if (!button) {
        return;
    }

    state.filter = button.dataset.filter || "all";
    for (const chip of filterRow.querySelectorAll("[data-filter]")) {
        chip.classList.toggle("active", chip === button);
    }
    renderClients();
});

clientForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.selectedClientId) {
        return;
    }

    try {
        setFormStatus("Guardando cambios…", true);
        const { response, payload } = await fetchJson(`/api/clients/${encodeURIComponent(state.selectedClientId)}`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                display_name: displayNameInput?.value || "",
                age: ageInput?.value || "",
            }),
        });
        if (!response.ok) {
            throw new Error(payload.error || "No se pudo guardar el cliente.");
        }

        setFormStatus("Cambios guardados.", true);
        await refreshDashboard();
    } catch (error) {
        setFormStatus(error.message || "No se pudo guardar el cliente.", false);
    }
});

clearHistoryButton?.addEventListener("click", async () => {
    if (!state.selectedClientId || !state.selectedClient) {
        return;
    }

    const confirmed = window.confirm(
        `Se borrará el histórico de ${state.selectedClient.display_name}. El cliente seguirá registrado.`
    );
    if (!confirmed) {
        return;
    }

    try {
        setConnectionStatus("Borrando histórico…", true);
        const { response, payload } = await fetchJson(`/api/clients/${encodeURIComponent(state.selectedClientId)}/history`, {
            method: "DELETE",
        });
        if (!response.ok) {
            throw new Error(payload.error || "No se pudo borrar el histórico.");
        }

        await refreshDashboard();
        setConnectionStatus("Histórico borrado", true);
    } catch (error) {
        setConnectionStatus(error.message || "No se pudo borrar el histórico.", false);
    }
});

deleteClientButton?.addEventListener("click", async () => {
    if (!state.selectedClientId || !state.selectedClient) {
        return;
    }

    const confirmed = window.confirm(
        `Vas a eliminar a ${state.selectedClient.display_name} y todo su histórico. Esta acción no se puede deshacer.`
    );
    if (!confirmed) {
        return;
    }

    try {
        setConnectionStatus("Eliminando cliente…", true);
        const { response, payload } = await fetchJson(`/api/clients/${encodeURIComponent(state.selectedClientId)}`, {
            method: "DELETE",
        });
        if (!response.ok) {
            throw new Error(payload.error || "No se pudo eliminar el cliente.");
        }

        state.selectedClientId = null;
        state.selectedClient = null;
        await refreshDashboard();
        setConnectionStatus("Cliente eliminado", true);
    } catch (error) {
        setConnectionStatus(error.message || "No se pudo eliminar el cliente.", false);
    }
});

async function refreshDashboard() {
    try {
        const [overviewResult, clientsResult] = await Promise.all([
            fetchJson("/api/admin/overview", { cache: "no-store" }),
            fetchJson("/api/clients", { cache: "no-store" }),
        ]);
        const overviewResponse = overviewResult.response;
        const clientsResponse = clientsResult.response;
        const overview = overviewResult.payload;
        const clients = clientsResult.payload;

        if (!overviewResponse.ok || !clientsResponse.ok) {
            throw new Error("No se pudo cargar la web de administrador.");
        }

        state.clients = clients;
        renderOverview(overview);
        ensureSelectedClient();
        renderClients();
        renderRecentActivity(overview.recent_activity || []);

        if (state.selectedClientId) {
            await loadClientDetails(state.selectedClientId);
        } else {
            renderEmptyClientState();
        }

        setConnectionStatus("Conectado", true);
    } catch (error) {
        if (error instanceof LoginRequiredError) {
            return;
        }
        console.error(error);
        setConnectionStatus("Servidor no disponible", false);
    }
}

function renderOverview(overview) {
    const stats = overview?.stats || {};
    statTotalClients.textContent = String(stats.total_clients || 0);
    statNewestClient.textContent = stats.newest_client_at
        ? `Alta más reciente: ${formatDate(stats.newest_client_at)}`
        : "Sin registros";

    statOnlineClients.textContent = String(stats.online_clients || 0);
    statOfflineClients.textContent = `${stats.offline_clients || 0} sin conexión`;

    statTotalLocations.textContent = String(stats.total_locations || 0);
    statLastSync.textContent = stats.latest_sync_at
        ? `Última sync: ${formatRelativeTime(stats.latest_sync_at)}`
        : "Sin sincronizar";

    statClientsWithLocation.textContent = String(stats.clients_with_location || 0);
    statDatabasePath.textContent = stats.database_path || "Sin ruta";
}

function renderRecentActivity(items) {
    if (!recentActivity) {
        return;
    }

    if (!items.length) {
        recentActivity.innerHTML = '<div class="empty-state">Todavía no hay actividad reciente.</div>';
        return;
    }

    recentActivity.innerHTML = "";
    for (const item of items) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "activity-item";
        button.innerHTML = `
            <div class="activity-top">
                <strong>${escapeHtml(item.display_name || "Cliente")}</strong>
                <span>${escapeHtml(formatRelativeTime(item.recorded_at))}</span>
            </div>
            <div class="activity-bottom">
                <span>${escapeHtml(formatCoordinatePair(item.latitude, item.longitude))}</span>
                <span>${escapeHtml(item.device_model || "Android")}</span>
            </div>
        `;
        button.addEventListener("click", () => {
            state.selectedClientId = item.client_id;
            renderClients();
            void loadClientDetails(item.client_id);
        });
        recentActivity.appendChild(button);
    }
}

function ensureSelectedClient() {
    if (!state.clients.length) {
        state.selectedClientId = null;
        return;
    }

    const selectedStillExists = state.clients.some((client) => client.id === state.selectedClientId);
    if (!state.selectedClientId || !selectedStillExists) {
        state.selectedClientId = state.clients[0].id;
    }
}

function getVisibleClients() {
    return state.clients.filter((client) => {
        if (state.filter === "online" && !client.is_online) {
            return false;
        }
        if (state.filter === "offline" && client.is_online) {
            return false;
        }

        if (!state.searchQuery) {
            return true;
        }

        const haystack = [
            client.display_name,
            client.device_model,
            client.id,
            client.age,
        ]
            .join(" ")
            .toLowerCase();

        return haystack.includes(state.searchQuery);
    });
}

function renderClients() {
    if (!clientsList) {
        return;
    }

    const visibleClients = getVisibleClients();
    if (!visibleClients.length) {
        clientsList.innerHTML = '<div class="empty-state">No hay clientes que coincidan con el filtro actual.</div>';
        return;
    }

    clientsList.innerHTML = "";
    for (const client of visibleClients) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `client-card${client.id === state.selectedClientId ? " active" : ""}`;
        button.innerHTML = `
            <div class="client-card-top">
                <div>
                    <h3>${escapeHtml(client.display_name)}</h3>
                    <p>${escapeHtml(client.device_model || "Android")}</p>
                </div>
                <span class="status-badge ${client.is_online ? "online" : "offline"}">
                    ${client.is_online ? "En línea" : "Sin conexión"}
                </span>
            </div>
            <div class="client-card-bottom">
                <span>${escapeHtml(client.age ? `${client.age} años` : "Edad no indicada")}</span>
                <span>${escapeHtml(formatRelativeTime(client.last_seen_at))}</span>
            </div>
        `;
        button.addEventListener("click", () => {
            state.selectedClientId = client.id;
            renderClients();
            void loadClientDetails(client.id);
        });
        clientsList.appendChild(button);
    }
}

async function loadClientDetails(clientId) {
    if (!clientId) {
        return;
    }

    try {
        const { response, payload } = await fetchJson(`/api/clients/${encodeURIComponent(clientId)}?limit=100`, {
            cache: "no-store",
        });
        if (!response.ok) {
            throw new Error(payload.error || "No se pudo cargar el detalle del cliente.");
        }

        state.selectedClient = payload.client;
        state.selectedHistory = payload.history || [];
        state.selectedMetrics = payload.metrics || null;
        renderClientDetails(payload.client, payload.history || [], payload.metrics || {});
    } catch (error) {
        if (error instanceof LoginRequiredError) {
            return;
        }
        console.error(error);
        setFormStatus(error.message || "No se pudo cargar el cliente.", false);
    }
}

function renderClientDetails(client, history, metrics) {
    clientName.textContent = client.display_name || "Cliente";
    clientMeta.textContent = `${client.device_model || "Android"} · ${formatRelativeTime(client.last_seen_at)}`;

    const coordinates = client.last_latitude != null && client.last_longitude != null
        ? formatCoordinatePair(client.last_latitude, client.last_longitude)
        : "Sin posición";
    const accuracy = client.last_accuracy != null ? `${Math.round(client.last_accuracy)} m` : "Sin dato";
    const battery = client.last_battery_percent != null
        ? `${client.last_battery_percent}%${client.last_is_charging ? " · cargando" : ""}`
        : "Sin dato";
    const totalPoints = metrics.total_points || 0;

    detailCoordinates.textContent = coordinates;
    detailAccuracy.textContent = accuracy;
    detailBattery.textContent = battery;
    detailHistory.textContent = `${history.length}/${totalPoints} puntos cargados`;

    if (displayNameInput) {
        displayNameInput.value = client.display_name || "";
    }
    if (ageInput) {
        ageInput.value = client.age || "";
    }

    clientForm?.classList.remove("hidden");
    clearHistoryButton?.classList.remove("hidden");
    deleteClientButton?.classList.remove("hidden");
    setFormStatus("Puedes editar el nombre o la edad desde aquí.", true);

    if (client.last_latitude != null && client.last_longitude != null) {
        const externalUrl = `https://www.openstreetmap.org/?mlat=${client.last_latitude}&mlon=${client.last_longitude}#map=17/${client.last_latitude}/${client.last_longitude}`;
        openMapsLink.href = externalUrl;
        openMapsLink.classList.remove("hidden");
    } else {
        openMapsLink.classList.add("hidden");
    }

    renderHistoryTable(history);
    updateMap(client, history);
}

function renderHistoryTable(history) {
    if (!historyTableBody) {
        return;
    }

    if (!history.length) {
        historyTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="table-empty">Este cliente todavía no tiene puntos guardados.</td>
            </tr>
        `;
        return;
    }

    historyTableBody.innerHTML = history
        .slice()
        .reverse()
        .map((point) => {
            const battery = point.battery_percent != null
                ? `${point.battery_percent}%${point.is_charging ? " · cargando" : ""}`
                : "Sin dato";

            return `
                <tr>
                    <td>${escapeHtml(formatDate(point.recorded_at))}</td>
                    <td>${escapeHtml(formatCoordinatePair(point.latitude, point.longitude))}</td>
                    <td>${escapeHtml(point.accuracy != null ? `${Math.round(point.accuracy)} m` : "Sin dato")}</td>
                    <td>${escapeHtml(battery)}</td>
                    <td>${escapeHtml(point.provider || "Sin dato")}</td>
                </tr>
            `;
        })
        .join("");
}

function renderEmptyClientState() {
    state.selectedClient = null;
    state.selectedHistory = [];
    state.selectedMetrics = null;

    clientName.textContent = "Sin cliente seleccionado";
    clientMeta.textContent = "Cuando un teléfono cliente se registre aparecerá aquí.";
    detailCoordinates.textContent = "Sin posición";
    detailAccuracy.textContent = "Sin dato";
    detailBattery.textContent = "Sin dato";
    detailHistory.textContent = "0 puntos";
    clientForm?.classList.add("hidden");
    openMapsLink?.classList.add("hidden");
    clearHistoryButton?.classList.add("hidden");
    deleteClientButton?.classList.add("hidden");
    setFormStatus("Selecciona un cliente para editarlo.", true);
    renderHistoryTable([]);
    clearMap();
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
            color: "#1b7f63",
            weight: 5,
            opacity: 0.82,
            lineJoin: "round",
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

function setConnectionStatus(text, ok = true) {
    if (!connectionPill) {
        return;
    }

    connectionPill.textContent = text;
    connectionPill.classList.toggle("warning", !ok);
}

function setFormStatus(text, ok = true) {
    if (!formStatus) {
        return;
    }

    formStatus.textContent = text;
    formStatus.classList.toggle("error", !ok);
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

function formatCoordinatePair(latitude, longitude) {
    if (latitude == null || longitude == null) {
        return "Sin posición";
    }
    return `${Number(latitude).toFixed(6)}, ${Number(longitude).toFixed(6)}`;
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
}, window.EASY2_DASHBOARD?.embedded ? 10000 : 7000);
