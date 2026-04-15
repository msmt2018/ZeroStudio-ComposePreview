# classes6.dex

.class public final Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;
.super Ljava/lang/Object;
.source "M3PreferenceBinding.java"

# interfaces
.implements Landroidx/viewbinding/ViewBinding;


# instance fields
.field public final empty:Lcom/google/android/material/card/MaterialCardView;

.field public final icon:Landroidx/appcompat/widget/AppCompatImageView;

.field public final iconFrame:Landroidx/appcompat/widget/LinearLayoutCompat;

.field private final rootView:Lcom/google/android/material/card/MaterialCardView;

.field public final summary:Lcom/google/android/material/textview/MaterialTextView;

.field public final title:Lcom/google/android/material/textview/MaterialTextView;

.field public final widgetFrame:Landroidx/appcompat/widget/LinearLayoutCompat;


# direct methods
.method private constructor <init>(Lcom/google/android/material/card/MaterialCardView;Lcom/google/android/material/card/MaterialCardView;Landroidx/appcompat/widget/AppCompatImageView;Landroidx/appcompat/widget/LinearLayoutCompat;Lcom/google/android/material/textview/MaterialTextView;Lcom/google/android/material/textview/MaterialTextView;Landroidx/appcompat/widget/LinearLayoutCompat;)V
    .registers 8
    .param p1, "rootView"  # Lcom/google/android/material/card/MaterialCardView;
    .param p2, "empty"  # Lcom/google/android/material/card/MaterialCardView;
    .param p3, "icon"  # Landroidx/appcompat/widget/AppCompatImageView;
    .param p4, "iconFrame"  # Landroidx/appcompat/widget/LinearLayoutCompat;
    .param p5, "summary"  # Lcom/google/android/material/textview/MaterialTextView;
    .param p6, "title"  # Lcom/google/android/material/textview/MaterialTextView;
    .param p7, "widgetFrame"  # Landroidx/appcompat/widget/LinearLayoutCompat;

    .line 45
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 46
    iput-object p1, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->rootView:Lcom/google/android/material/card/MaterialCardView;

    .line 47
    iput-object p2, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->empty:Lcom/google/android/material/card/MaterialCardView;

    .line 48
    iput-object p3, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->icon:Landroidx/appcompat/widget/AppCompatImageView;

    .line 49
    iput-object p4, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->iconFrame:Landroidx/appcompat/widget/LinearLayoutCompat;

    .line 50
    iput-object p5, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->summary:Lcom/google/android/material/textview/MaterialTextView;

    .line 51
    iput-object p6, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->title:Lcom/google/android/material/textview/MaterialTextView;

    .line 52
    iput-object p7, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->widgetFrame:Landroidx/appcompat/widget/LinearLayoutCompat;

    .line 53
    return-void
.end method

.method public static bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;
    .registers 10
    .param p0, "rootView"  # Landroid/view/View;

    .line 82
    move-object v2, p0

    check-cast v2, Lcom/google/android/material/card/MaterialCardView;

    .line 84
    .local v2, "empty":Lcom/google/android/material/card/MaterialCardView;
    const v0, 0x1020006

    .line 85
    .local v0, "id":I
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    move-object v3, v1

    check-cast v3, Landroidx/appcompat/widget/AppCompatImageView;

    .line 86
    .local v3, "icon":Landroidx/appcompat/widget/AppCompatImageView;
    if-eqz v3, :cond_4c

    .line 90
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->icon_frame:I

    .line 91
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    move-object v4, v1

    check-cast v4, Landroidx/appcompat/widget/LinearLayoutCompat;

    .line 92
    .local v4, "iconFrame":Landroidx/appcompat/widget/LinearLayoutCompat;
    if-eqz v4, :cond_4b

    .line 96
    const v0, 0x1020010

    .line 97
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    move-object v5, v1

    check-cast v5, Lcom/google/android/material/textview/MaterialTextView;

    .line 98
    .local v5, "summary":Lcom/google/android/material/textview/MaterialTextView;
    if-eqz v5, :cond_4a

    .line 102
    const v0, 0x1020016

    .line 103
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    move-object v6, v1

    check-cast v6, Lcom/google/android/material/textview/MaterialTextView;

    .line 104
    .local v6, "title":Lcom/google/android/material/textview/MaterialTextView;
    if-eqz v6, :cond_49

    .line 108
    const v8, 0x1020018

    .line 109
    .end local v0  # "id":I
    .local v8, "id":I
    invoke-static {p0, v8}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v0

    move-object v7, v0

    check-cast v7, Landroidx/appcompat/widget/LinearLayoutCompat;

    .line 110
    .local v7, "widgetFrame":Landroidx/appcompat/widget/LinearLayoutCompat;
    if-eqz v7, :cond_47

    .line 114
    new-instance v0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;

    move-object v1, p0

    check-cast v1, Lcom/google/android/material/card/MaterialCardView;

    invoke-direct/range {v0 .. v7}, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;-><init>(Lcom/google/android/material/card/MaterialCardView;Lcom/google/android/material/card/MaterialCardView;Landroidx/appcompat/widget/AppCompatImageView;Landroidx/appcompat/widget/LinearLayoutCompat;Lcom/google/android/material/textview/MaterialTextView;Lcom/google/android/material/textview/MaterialTextView;Landroidx/appcompat/widget/LinearLayoutCompat;)V

    return-object v0

    .line 111
    :cond_47
    move v0, v8

    goto :goto_4d

    .line 105
    .end local v7  # "widgetFrame":Landroidx/appcompat/widget/LinearLayoutCompat;
    .end local v8  # "id":I
    .restart local v0  # "id":I
    :cond_49
    goto :goto_4d

    .line 99
    .end local v6  # "title":Lcom/google/android/material/textview/MaterialTextView;
    :cond_4a
    goto :goto_4d

    .line 93
    .end local v5  # "summary":Lcom/google/android/material/textview/MaterialTextView;
    :cond_4b
    goto :goto_4d

    .line 87
    .end local v4  # "iconFrame":Landroidx/appcompat/widget/LinearLayoutCompat;
    :cond_4c
    nop

    .line 117
    .end local v2  # "empty":Lcom/google/android/material/card/MaterialCardView;
    .end local v3  # "icon":Landroidx/appcompat/widget/AppCompatImageView;
    :goto_4d
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object v1

    .line 118
    .local v1, "missingId":Ljava/lang/String;
    new-instance v2, Ljava/lang/NullPointerException;

    const-string v3, "Missing required view with ID: "

    invoke-virtual {v3, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw v2
.end method

.method public static inflate(Landroid/view/LayoutInflater;)Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;
    .registers 3
    .param p0, "inflater"  # Landroid/view/LayoutInflater;

    .line 63
    const/4 v0, 0x0

    const/4 v1, 0x0

    invoke-static {p0, v0, v1}, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;

    move-result-object v0

    return-object v0
.end method

.method public static inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;
    .registers 5
    .param p0, "inflater"  # Landroid/view/LayoutInflater;
    .param p1, "parent"  # Landroid/view/ViewGroup;
    .param p2, "attachToParent"  # Z

    .line 69
    sget v0, Landroid/zero/studio/compose/preview/resources/R$layout;->m3_preference:I

    const/4 v1, 0x0

    invoke-virtual {p0, v0, p1, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    .line 70
    .local v0, "root":Landroid/view/View;
    if-eqz p2, :cond_c

    .line 71
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    .line 73
    :cond_c
    invoke-static {v0}, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;

    move-result-object v1

    return-object v1
.end method


# virtual methods
.method public bridge synthetic getRoot()Landroid/view/View;
    .registers 2

    .line 20
    invoke-virtual {p0}, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->getRoot()Lcom/google/android/material/card/MaterialCardView;

    move-result-object v0

    return-object v0
.end method

.method public getRoot()Lcom/google/android/material/card/MaterialCardView;
    .registers 2

    .line 58
    iget-object v0, p0, Landroid/zero/studio/compose/preview/resources/databinding/M3PreferenceBinding;->rootView:Lcom/google/android/material/card/MaterialCardView;

    return-object v0
.end method
