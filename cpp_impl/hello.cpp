#include<iostream>
#include<cmath>
#include<vector>

using namespace std;

#define pb push_back
#define sz(x) x.size()
#define rep(i, a, n) for(int (i)=a; (i)<(n); (i)++)
#define FOR(i, n) rep(i, 0, n)
#define F(n) FOR(i, n)
#define FF(n) FOR(j, n)
#define repi(i, a, n) for(int (i)=a; (i)<=(n); (i)++)
#define FORI(i, n) repi(i, 1, n)
#define FI(n) FORI(i, n)
#define FFI(n) FORI(j, n)
#define IN(x) int x; cin>>(x);
#define IL(x) ll x; cin>>(x);
#define SIN(x) string x; cin>>(x);
#define INN(x, y) int x, y; cin>>(x)>>(y);
#define ILL(x, y) ll x, y; cin>>(x)>>(y);
#define vi vector<int>
#define vll vector<ll>
#define pii pair<int, int>
#define pff pair<double, double>
#define vpii vector<pair<int, int> >
#define all(v) v.begin(), v.end()
#define fi first
#define se second
#define mp make_pair
#define log(x) cout<<#x<<" : "<<x<<endl;
#define logp(x) cout<<#x<<" : ("<<x.fi<<", "<<x.se<<")"<<endl;
#define logi(a, i) cout<<#a<<'['<<i<<']'<<" : "<<a[i]<<endl;
#define loga(a, n) cout<<#a<<" -> "; rep(z, 0, n) cout<<z<<": "<<a[z]<<", "; cout<<endl;
#define logv(a) loga(a, a.size())
#define out(x) cout<<x<<endl
#define outp(x) cout<<x.fi<<' '<<x.se<<endl
#define outa(a, n) rep(z, 0, n) cout<<a[z]<<' '; cout<<endl;
#define outv(a) outa(a, a.size())
#define sim template<class t
typedef long long ll;
typedef long double LD;


struct gameField {
    int width, height;
    int leftGoal[4], rightGoal[4];
} field;

struct ball {
    int id, radius, mass, player;
    pff pos, vel;
} ball1, ball2, ball3, ball4, ball5, ball6, king;

struct world {
    vector<ball*> balls;
    bool stable;
} state;

pff addVector(pff v1, pff v2) {
    return mp(v1.fi + v2.fi, v1.se + v2.se);
}

pff subVector(pff v1, pff v2) {
    return mp(v1.fi - v2.fi, v1.se - v2.se);
}

double dotVector(pff v1, pff v2) {
    return v1.fi*v2.fi + v1.se*v2.se;
}

double absVector (pff v) {
    auto vx = v.fi;
    auto vy = v.se;
    return sqrt(vx*vx + vy*vy);
}

pff scaleVector(pff v, double a) {
    return mp(v.fi*a, v.se*a);
}

double disVector(pff v1, pff v2) {
    auto v = mp(v1.fi - v2.fi, v1.se - v2.se);
    return absVector(v);
}

pff newPos(pff oldPos, pff v) {
    return addVector(oldPos, v);
}

pff ballSide(ball b, pff dir) {
    return addVector(b.pos, scaleVector(dir, (double) b.radius));
}

bool checkOutField(pff pos) {
    double px = pos.fi, py = pos.se;
    return !(px > 0 && px < field.width && py > 0 && py < field.height);
}

void updateWallHitDir(ball *b, pff dir) {
    if(!checkOutField(ballSide(*b, dir))) return;
    pff vel = b->vel;
    pff x = mp(abs(vel.fi) * dir.fi * -2, abs(vel.se) * dir.se * -2);
    b->vel = addVector(vel, x);
}

void updateWallHit(ball *b) {
    updateWallHitDir(b, mp(-1, 0));
    updateWallHitDir(b, mp(1, 0));
    updateWallHitDir(b, mp(0, -1));
    updateWallHitDir(b, mp(0, 1));
}

bool checkCollision(ball b1, ball b2) {
    double x = disVector(b1.pos, b2.pos);
    double y = b1.radius + b2.radius;
    return x <= y;
}

void updateBallVelCollision(ball *b1, ball *b2) {
    if(!checkCollision(*b1, *b2)) return;
    pff v1 = b1->vel, p1 = b1->pos; int m1 = b1->mass;
    pff v2 = b2->vel, p2 = b2->pos; int m2 = b2->mass;
    pff xdif = subVector(p1, p2);
    double x = sqrt(absVector(xdif));
    double y = dotVector(subVector(v1, v2), xdif);
    x = x / y;
    y = (2*m2) / (m1+m2);
    pff x1 = scaleVector(xdif, x*y);
    b1->vel = subVector(v1, x1);
}

void updateBallCollision(ball *b, const vector<ball*> &balls) {
    for(auto b2: balls) {
        if(b->id != b2->id) {
            updateBallVelCollision(b, b2);
        }
    }
}

#define damp 0.98

pff newVel(pff vel) {
    double vx = vel.fi, vy = vel.se;
    if(abs(vx) > 0.01) vx *= damp;
    else vx = 0;
    if(abs(vy) > 0.01) vy *= damp;
    else vy = 0;
    return mp(vx, vy);
}

void updateBallVecs(ball *b) {
    b->pos = newPos(b->pos, b->vel);
    b->vel = newVel(b->vel);
}

void updateBalls(vector<ball*> balls) {
    for(auto &b: balls) {
        updateBallVecs(b);
    }
}

bool checkStable(const vector<ball*> &balls) {
    for(auto &b: balls) {
        if((b->vel).fi != 0 || (b->vel).se != 0) {
            return false;
        }
    }
    return true;
}

void updateState(world *s) {
    if(s->stable) return;
    updateBalls(s->balls);
    s->stable = checkStable(s->balls);
}

void setField() {
    field.width = 480, field.height = 320;
}

void setStartingState() {
    ball1.id = 1, ball1.pos = mp(80, 60), ball1.vel = mp(2, 2), ball1.radius = 15, ball1.mass = 15* 15, ball1.player = 1;
    ball2.id = 2, ball2.pos = mp(110, 160), ball2.vel = mp(0, 0), ball2.radius = 15, ball2.mass = 15 * 15, ball2.player = 1;
    ball3.id = 3, ball3.pos = mp(80, 260), ball3.vel = mp(0, 0), ball3.radius = 15, ball3.mass = 15 * 15, ball3.player = 1;
    ball4.id = 4, ball4.pos = mp(400, 60),ball4.vel = mp(1, 1), ball4.radius = 15, ball4.mass = 15 * 15, ball4.player = 2;
    ball5.id = 5, ball5.pos = mp(370, 160), ball5.vel = mp(1, 1), ball5.radius = 15, ball5.mass = 15 * 15, ball5.player = 2;
    ball6.id = 6, ball6.pos = mp(400, 260), ball6.vel = mp(1, 1), ball6.radius = 15, ball6.mass = 15 * 15, ball6.player = 2;
    king.id = 7, king.pos = mp(240, 160), king.vel = mp(0, 0), king.radius = 10, king.mass = 10 * 10, king.player = 0;
    state.balls.pb(&ball1);
    state.balls.pb(&ball2);
    state.balls.pb(&ball3);
    state.balls.pb(&ball4);
    state.balls.pb(&ball5);
    state.balls.pb(&ball6);
    state.balls.pb(&king);
    state.stable = false;
}

int main() {
    time_t start, end;
    time(&start);
    for(int i=0; i<2000; i++) {
        setStartingState();
        while(!state.stable) updateState(&state);
        //cout<<(state.balls[0]->pos).fi<<endl;
    }
    time(&end);
    cout<<start<<endl;
    cout<<end<<endl;
    double time_taken = double(end - start); 
    cout << "Time taken by program is : " << time_taken;
    cout << " sec " << endl; 
    return 0; 
}
