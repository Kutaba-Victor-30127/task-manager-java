// ================= GLOBAL STATE =================
let token = localStorage.getItem("token") || null;
let role = null;

let debounceTimeout;

let currentPage = 0;
let pageSize = 5;
let totalPages = 0;
let searchText = "";
let filterStatus = "";
let sortField = "id";
let sortDirection = "asc";

// ================= INIT AUTH (AUTO LOGIN) =================
initAuth();

function initAuth() {
    if (token) {
        const payload = parseJwt(token);
        role = payload?.role;

        document.getElementById("tasks").style.display = "block";
        loadTasks();
    } else {
        document.getElementById("tasks").style.display = "none";
    }
}

// ================= JWT PARSER =================
function parseJwt(token){
    try {
        return JSON.parse(atob(token.split(".")[1]));
    } catch (e) {
        return null;
    }   
}

// ================= LOGIN =================
async function login() {

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    try{
        const response = await fetch("https://task-manager-java-zrc8.onrender.com/api/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({username, password})
        });

        if(!response.ok){
            localStorage.removeItem("token");

            token = null;
            role = null;

            const list = document.getElementById("tasks");
            if (list) list.innerHTML = "";

            alert("Login failed");
            return;
        }

        const data = await response.json();
        
        token = data.token;
        localStorage.setItem("token", token);

        const payload = parseJwt(token);
        role = payload.role;

        alert("Login successful");

        currentPage = 0;
        loadTasks();

    } catch (err) {
        console.error("Login error: ", err);

        alert("Server error (CORS / backend down)");

        localStorage.removeItem("token");
        token = null;
        role = null;
    }
}

// ================= LOGOUT =================
function logout() {
    localStorage.removeItem("token");

    token = null;
    role = null;

    const list = document.getElementById("tasks");
    if (list) list.innerHTML = "";

    alert("Logged out");
}

// ================= LOAD TASKS =================
async function loadTasks() {

    const list = document.getElementById("tasks");
    if (!list){
        console.error("tasks element not found");
        return;
    }

    if (!token) {
        list.innerHTML = "<p>Please login to see tasks</p>";
        return;
    }

    setLoading(true);

    try {

        list.innerHTML = "";

        for (let i = 0; i < 5; i++) {
            const sk = document.createElement("div");
            sk.className = "skeleton";
            list.appendChild(sk);
        }

        const query = new URLSearchParams({
            page: currentPage,
            size: pageSize,
            text: searchText,
            status: filterStatus,
            sort: sortField,
            direction: sortDirection
        });

        const url = role === "ADMIN"
            ? `https://task-manager-java-zrc8.onrender.com/api/admin/all-tasks?${query.toString()}`
            : `https://task-manager-java-zrc8.onrender.com/api/tasks?${query.toString()}`;

        const response = await fetch(url, { 
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if(!response.ok){
            list.innerHTML = "Error loading tasks";
            return;
        }

        const data = await response.json();
        const tasks = data.content;
        totalPages = data.totalPages;

        list.innerHTML = ""; 

        tasks.forEach(t => {
            const div = document.createElement("div");
            div.className = "task";

            div.innerHTML = `
                <span>
                <b>${t.title}</b><br> 
                <small>${t.status}</small>
                </span>
                ${role === "ADMIN" ? `<button onclick="deleteTask(${t.id})">Delete</button>` : ""}  
            `;

            list.appendChild(div);
        });

        updatePaginationUI();

    } catch (e) {
        console.error(e);
        list.innerHTML = "Error loading tasks";
    } finally {
        setLoading(false); 
    }
}

// ================= CREATE =================
async function createTask() {
    
    if (!token) {
        const list = document.getElementById("tasks");
        if (list) list.innerHTML = "<p>Please login to see tasks</p>";
        return;
    }

    const title = document.getElementById("task-title").value;
    const deadlineRaw = document.getElementById("task-deadline").value;
    const deadline = deadlineRaw ? deadlineRaw.split("T")[0] : null;

    const response = await fetch("https://task-manager-java-zrc8.onrender.com/api/tasks", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        },
        body: JSON.stringify({
            title, 
            description: "default",
            priority: 3,
            deadline,
            status: "TODO",
            estimatedMinutes: 30
        })
    });

    if (!response.ok){
        const list = document.getElementById("tasks");
        if (list) list.innerHTML = "<p>Failed to create task</p>";
        return;
    }

    document.getElementById("task-title").value = "";
    document.getElementById("task-deadline").value = "";

    currentPage = 0;
    loadTasks();
}

// ================= DELETE =================
async function deleteTask(id){
    
    if (!token) {
        const list = document.getElementById("tasks");
        if (list) list.innerHTML = "<p>Please login to perform this action</p>";
        return;
    }
    
    const response = await fetch(`https://task-manager-java-zrc8.onrender.com/api/tasks/${id}`, { 
        method: "DELETE",
        headers: {
            "Authorization": "Bearer " + token
        }
    });

    if(!response.ok){
        const list = document.getElementById("tasks");
        if (list) list.innerHTML = "<p>Failed to delete task</p>";
        return;
    }

    loadTasks();
}

// ================= PAGINATION =================
function nextPage(){
    if (currentPage < totalPages - 1){
        currentPage++;
        loadTasks();
    }
}

function prevPage(){
    if (currentPage > 0){
        currentPage--;
        loadTasks();
    }
}

function updatePaginationUI(){
    const info = document.getElementById("page-info");
    if (info){
        info.innerText = `Page ${currentPage + 1} / ${totalPages}`;
    }   
}

// ================= FILTER =================
function applyFilters(){
    searchText = document.getElementById("search").value;
    filterStatus = document.getElementById("status").value;
    sortField = document.getElementById("sort").value;
    sortDirection = document.getElementById("direction").value;
    
    currentPage = 0;
    loadTasks();
}

function debounceLoadTasks(){
    clearTimeout(debounceTimeout);
 
    debounceTimeout = setTimeout(() => {
        currentPage = 0;
        loadTasks();
    }, 400);
}

// ================= LOADING =================
function setLoading(isLoading) {
    const loading = document.getElementById("loading");

    if (loading) {
        loading.style.display = isLoading ? "block" : "none";
    }
}